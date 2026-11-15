package pl.nclean.crm.work;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class Navigation {
    private Navigation(){}
    public static String destination(String address){return address==null||address.isBlank()?null:"https://www.google.com/maps/dir/?api=1&travelmode=driving&destination="+URLEncoder.encode(address.strip(),StandardCharsets.UTF_8);}
}
