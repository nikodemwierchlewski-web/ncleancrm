package pl.nclean.crm.fuel;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Re-encoded private JPEGs in PostgreSQL: no user-controlled paths or public static files. */
@Service
@Transactional(readOnly=true)
public class ReceiptService {
    public static final int MAX_BYTES=8*1024*1024;
    private final JdbcTemplate db;
    public ReceiptService(JdbcTemplate db){this.db=db;}
    @Transactional public UUID upload(MultipartFile file){
        byte[] image=normalize(file);db.queryForObject("SELECT id FROM fuel_write_guard WHERE id=1 FOR UPDATE",Integer.class);UUID id=UUID.randomUUID();db.update("INSERT INTO fuel_receipt(id,image) VALUES (?,?)",id,image);
        db.update("DELETE FROM fuel_receipt r WHERE created_at<CURRENT_TIMESTAMP-INTERVAL '24 hours' AND NOT EXISTS(SELECT 1 FROM fuel_purchase p WHERE p.receipt_id=r.id)");return id;
    }
    public byte[] normalize(MultipartFile file){
        if(file==null||file.isEmpty()||file.getSize()>MAX_BYTES)throw new IllegalArgumentException("Wybierz zdjęcie JPEG lub PNG do 8 MB.");
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))){
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IllegalArgumentException("Plik nie jest poprawnym zdjęciem JPEG lub PNG.");
            var reader=readers.next();try{
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);if(!Set.of("jpeg","jpg","png").contains(format))throw new IllegalArgumentException("Obsługiwane są zdjęcia JPEG i PNG. Zapisz HEIC jako JPEG.");
                reader.setInput(input,true,true);int width=reader.getWidth(0),height=reader.getHeight(0);if(width<=0||height<=0||(long)width*height>20_000_000)throw new IllegalArgumentException("Zdjęcie może mieć maksymalnie 20 megapikseli.");
                var image=reader.read(0);var normalized=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);var graphics=normalized.createGraphics();try{graphics.setColor(Color.WHITE);graphics.fillRect(0,0,width,height);graphics.drawImage(image,0,0,null);}finally{graphics.dispose();}
                var bytes=new ByteArrayOutputStream();if(!ImageIO.write(normalized,"jpeg",bytes)||bytes.size()>MAX_BYTES)throw new IllegalArgumentException("Zdjęcie po przetworzeniu przekracza limit. Wybierz mniejszy obraz.");
                return bytes.toByteArray();
            }finally{reader.dispose();}
        }catch(IOException ex){throw new IllegalArgumentException("Nie udało się odczytać zdjęcia. Wybierz poprawny plik JPEG lub PNG.");}
    }
    public byte[] image(UUID id){return db.query("SELECT image FROM fuel_receipt WHERE id=?",(rs,n)->rs.getBytes(1),id).stream().findFirst().orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));}
    @Transactional public void discard(UUID id){db.queryForObject("SELECT id FROM fuel_write_guard WHERE id=1 FOR UPDATE",Integer.class);db.update("DELETE FROM fuel_receipt r WHERE id=? AND NOT EXISTS(SELECT 1 FROM fuel_purchase p WHERE p.receipt_id=r.id)",id);}
}
