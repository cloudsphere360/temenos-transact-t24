package com.tem.msg.fusion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temenos.fusion.FusionFileLogger;
import com.temenos.t24.api.records.ebfusionlogsmsupdate.EbFusionLogSmsUpdateRecord;
import com.temenos.t24.api.system.Session;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SendSmsUtil {
    private static final FusionFileLogger LOGGER = FusionFileLogger.getLogger(SendSmsUtil.class);

   public static void SendSms(String smsReqCreate, String apiUrl, EbFusionLogSmsUpdateRecord logRec, String description, String apiStatus) {
      ObjectMapper mapper = new ObjectMapper();
      StringBuilder sb = new StringBuilder();
      Session sn = new Session();
      String responseValue = "";
      LOGGER.info("apiUrl" + apiUrl);
      LOGGER.info("smsReqCreate" + smsReqCreate);
      

      try {
         URL url = new URL(apiUrl);
         HttpURLConnection con = (HttpURLConnection)url.openConnection();
         con.setRequestMethod("POST");
         con.setRequestProperty("content-type", "application/json");
         con.setRequestProperty("User-Agent", "Mozilla/5.0");
         con.setRequestProperty("Accept-Language", "UTF-8");
         con.setDoOutput(true);
         OutputStream os = con.getOutputStream();
         os.write(smsReqCreate.getBytes());
         os.flush();
         os.close();
         int responseCode = con.getResponseCode();
         LOGGER.info("responseCode" + responseCode);
         BufferedReader bfReader;
         if (responseCode != 200) {
            bfReader = new BufferedReader(new InputStreamReader(con.getErrorStream()));
         } else {
            bfReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
         }

         String inputLine;
         while((inputLine = bfReader.readLine()) != null) {
            sb.append(inputLine);
         }

         bfReader.close();
         responseValue = sb.toString();
         LOGGER.info("responseValue" + responseValue);

         try {
            JsonNode rootNode = mapper.readTree(responseValue);
            String status = rootNode.path("status").asText();
            LOGGER.info("status" + status);
            if (!status.equals("")) {
               new FfMessageFailResponseApi();
               FfMessageFailResponseApi apiFailResponse = (FfMessageFailResponseApi)mapper.readValue(responseValue, FfMessageFailResponseApi.class);
               description = apiFailResponse.getResponseMessage();
               apiStatus = "FAILED";
            } else {
               description = responseValue;
               apiStatus = "SUCCESS";
            }
         } catch (Exception var20) {
            description = responseValue;
            apiStatus = "SUCCESS";
         }

         LocalDate todayDate = LocalDate.now();
         DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
         String todayLocalDate = todayDate.format(formatter);
         logRec.getStatus().setValue(apiStatus);
         logRec.getRequest().setValue(smsReqCreate);
         logRec.getResponse().setValue(description);
         logRec.getDateTriggered().setValue(todayLocalDate);

         try {
            String Mne = sn.getCompanyRecord().getFinancialMne().toString();
            logRec.getReserved3().setValue(Mne);
         } catch (Exception e123) {
            LOGGER.info("e123" + e123);
         }

         logRec.getRetryCount().setValue("1");
         LOGGER.info("logRec"+logRec.toString());
      } catch (Exception e) {
         LOGGER.info("e80: " + e);
      }

   }
}