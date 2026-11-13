package pl.nclean.crm.fuel;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
class ImageOrientationTest {
    @Test void rotatesRealJpegPixelsAndRemovesExif() throws Exception {
        var image=new BufferedImage(120,60,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(Color.RED);g.fillRect(0,0,60,60);g.setColor(Color.BLUE);g.fillRect(60,0,60,60);g.dispose();
        var encoded=new ByteArrayOutputStream();ImageIO.write(image,"jpeg",encoded);byte[] jpeg=encoded.toByteArray();
        for(int orientation=1;orientation<=8;orientation++){
            byte[] metadata={(byte)255,(byte)225,0,34,'E','x','i','f',0,0,'I','I',42,0,8,0,0,0,1,0,18,1,3,0,1,0,0,0,(byte)orientation,0,0,0,0,0,0,0};
            var full=new ByteArrayOutputStream();full.write(jpeg,0,2);full.write(metadata);full.write(jpeg,2,jpeg.length-2);byte[] original=full.toByteArray();assertEquals(orientation,ImageOrientation.read(original));
            byte[] normalized=new ReceiptService(null).normalize(new MockMultipartFile("image","phone.jpg","image/jpeg",original));var result=ImageIO.read(new ByteArrayInputStream(normalized));
            assertEquals(orientation>=5?60:120,result.getWidth());assertEquals(orientation>=5?120:60,result.getHeight());assertEquals(1,ImageOrientation.read(normalized));
            boolean topLeftRed=switch(orientation){case 1,4,5,6->true;default->false;};var sample=new Color(result.getRGB(10,10));assertTrue(topLeftRed?sample.getRed()>sample.getBlue():sample.getBlue()>sample.getRed());
        }
    }
    @Test void rejectsDecodedResolutionBeforeAllocatingPixels() throws Exception {
        // Complete synthetic 6000x4000 PNG, generated from a solid color, no customer data.
        byte[] bytes;try(var stream=getClass().getResourceAsStream("/images/24mp-synthetic.png")){bytes=stream.readAllBytes();}
        var file=new MockMultipartFile("image","24mp.png","image/png",bytes);
        assertTrue(assertThrows(IllegalArgumentException.class,()->new ReceiptService(null).normalize(file)).getMessage().contains("20 megapikseli"));
    }
    @Test void malformedOffsetsAndHeicAreSafe() {
        assertEquals(1,ImageOrientation.read(new byte[]{(byte)255,(byte)216,(byte)255,(byte)225,127,127}));
        var invalid=new MockMultipartFile("image","photo.heic","image/heic",new byte[]{0,0,0,24,'f','t','y','p','h','e','i','c'});
        assertTrue(assertThrows(IllegalArgumentException.class,()->new ReceiptService(null).normalize(invalid)).getMessage().contains("HEIC"));
    }
}
