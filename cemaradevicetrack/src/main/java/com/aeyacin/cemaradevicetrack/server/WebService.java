package com.aeyacin.cemaradevicetrack.server;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

public class WebService {

    private static String NAMESPACE = "http://tempuri.org/";
    private static String URLRotaWebServis = "http://ats2.rota.net.tr/service/RotaService.asmx";
    private static final int WS_TIMEOUT = 10000;

    public static String CameraActive(String Path, String ID) {
        String METHOD_NAME = "CameraShake";
        String SOAP_ACTION = NAMESPACE + METHOD_NAME;
        SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);

        request.addProperty("PathName", "http://178.18.200.116:88/" + Path);
        request.addProperty("DeviceImei", ID);
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);

        envelope.dotNet = true;

        envelope.setOutputSoapObject(request);
        HttpTransportSE httpTransport = new HttpTransportSE(URLRotaWebServis, WS_TIMEOUT);
        SoapPrimitive result = null;

        String sonuc = "false";
        try {
            httpTransport.call(SOAP_ACTION, envelope);
            result = (SoapPrimitive) envelope.getResponse();

        } catch (Exception e) {
            e.toString();
            sonuc = "false";

        }

        return sonuc;


    }
}
