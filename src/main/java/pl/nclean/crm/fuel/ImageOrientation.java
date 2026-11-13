package pl.nclean.crm.fuel;

/** Reads only the bounded TIFF orientation in JPEG APP1; metadata is discarded on re-encoding. */
final class ImageOrientation {
    private ImageOrientation() {}
    static int read(byte[] bytes) {
        if(bytes.length<4||(bytes[0]&255)!=255||(bytes[1]&255)!=216)return 1;
        for(int p=2;p+4<=bytes.length;) {
            if((bytes[p++]&255)!=255)return 1;
            int marker=bytes[p++]&255;if(marker==218||marker==217)return 1;
            if(marker==255){p--;continue;}
            int length=((bytes[p]&255)<<8)|(bytes[p+1]&255);
            if(length<2||length>bytes.length-p)return 1;
            int base=p+8,end=p+length;
            if(marker==225&&length>=16&&bytes[p+2]=='E'&&bytes[p+3]=='x'&&bytes[p+4]=='i'&&bytes[p+5]=='f'&&bytes[p+6]==0&&bytes[p+7]==0) {
                boolean le=bytes[base]=='I'&&bytes[base+1]=='I';
                if(!le&&!(bytes[base]=='M'&&bytes[base+1]=='M'))return 1;
                if(number(bytes,base+2,2,le)!=42)return 1;
                long offset=number(bytes,base+4,4,le);
                if(offset<8||offset>end-base-2)return 1;
                int dir=base+(int)offset,count=(int)number(bytes,dir,2,le);
                for(int entry=dir+2,n=0;n<count&&entry<=end-12;n++,entry+=12) {
                    if(number(bytes,entry,2,le)==274&&number(bytes,entry+2,2,le)==3&&number(bytes,entry+4,4,le)==1) {
                        int value=(int)number(bytes,entry+8,2,le);return value>=1&&value<=8?value:1;
                    }
                }
            }
            p=end;
        }
        return 1;
    }
    private static long number(byte[] bytes,int p,int size,boolean little){
        long value=0;for(int i=0;i<size;i++)value=(value<<8)|(bytes[p+(little?size-1-i:i)]&255);return value;
    }
    static java.awt.geom.AffineTransform transform(int value,int width,int height){
        return switch(value){
            case 2->new java.awt.geom.AffineTransform(-1,0,0,1,width,0);
            case 3->new java.awt.geom.AffineTransform(-1,0,0,-1,width,height);
            case 4->new java.awt.geom.AffineTransform(1,0,0,-1,0,height);
            case 5->new java.awt.geom.AffineTransform(0,1,1,0,0,0);
            case 6->new java.awt.geom.AffineTransform(0,1,-1,0,height,0);
            case 7->new java.awt.geom.AffineTransform(0,-1,-1,0,height,width);
            case 8->new java.awt.geom.AffineTransform(0,-1,1,0,0,width);
            default->new java.awt.geom.AffineTransform();
        };
    }
}
