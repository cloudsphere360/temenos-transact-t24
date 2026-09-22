package com.temenos.fusion;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.ebffcollpostingscreen.EbFfCollPostingScreenRecord;
import com.temenos.t24.api.system.DataAccess;
//import com.temenos.t24.api.system.Session;
//import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author mp116300
 *
 */
public class FfTellerAutoCollScreen extends ServiceLifecycle {

    private static final FusionFileLogger FfTellerAutoCollScreen = FusionFileLogger
            .getLogger(FfTellerAutoCollScreen.class);

    private final DataAccess da = new DataAccess(this);
    public static final String INPUT = "INPUT";
    public static final String ZERO = "0";
    public static final String OFS_SOURCE = "AUTOCOLL.OFS";
    public static final String VERSION_COLL_SCREEN = "EB.FF.COLL.POSTING.SCREEN,OFS";

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        List<String> companyList = da.selectRecords("", "COMPANY", "", "");
        FfTellerAutoCollScreen.info("Company List " + companyList.toString());

        return companyList;
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {

        FfTellerAutoCollScreen.info("Service data " + serviceData.toString());
        FfTellerAutoCollScreen.info("Control Item " + controlItem.toString());

        FfTellerAutoCollScreen.info("id " + id);
        String todayDate = "";
        TStructure datesRec = da.getRecord("DATES", id);
        DatesRecord datesRecord = new DatesRecord(datesRec);
        String coBatchStatus = datesRecord.getCoBatchStatus().getValue();

        FfTellerAutoCollScreen.info("datesRec is : " + datesRec.toString());
        FfTellerAutoCollScreen.info("datesRecord is : " + datesRecord.toString());
        FfTellerAutoCollScreen.info("coBatchStatus is : " + coBatchStatus.toString());

        // -------------------------------------------------
        // IF BATCH STATUS = O
        // USE CURRENT COMPANY DATES RECORD
        // -------------------------------------------------

        if ("O".equalsIgnoreCase(coBatchStatus)) {

            todayDate = datesRecord.getToday().getValue();
            // yestDate = datesRecord.getLastWorkingDay().getValue();

            FfTellerAutoCollScreen.info("Using ONLINE dates record");

        } else {
            // -------------------------------------------------
            // ELSE READ COB COMPANY DATES RECORD
            // Example: IN-001-0001-COB
            // -------------------------------------------------
            String cobCompany = id + "-COB";

            FfTellerAutoCollScreen.info("Reading COB dates record : " + cobCompany);

            TStructure cobDatesRec = da.getRecord("DATES", cobCompany);

            DatesRecord cobDatesRecord = new DatesRecord(cobDatesRec);

            todayDate = cobDatesRecord.getToday().getValue();
            // yestDate = cobDatesRecord.getLastWorkingDay().getValue();

            FfTellerAutoCollScreen.info("Using COB dates record");
        }
        // todayDate = ses.getCurrentVariable("!TODAY");
        FfTellerAutoCollScreen.info("todayDate " + todayDate);
        String concatId = id + "-" + todayDate; // IN0011007-20251113
        FfTellerAutoCollScreen.info("concatId " + concatId);
        EbFfCollPostingScreenRecord ebCollPostRec = new EbFfCollPostingScreenRecord(this);
        SynchronousTransactionData yTransData = new SynchronousTransactionData();
        yTransData.setFunction(INPUT);
        yTransData.setCompanyId(id);
        yTransData.setNumberOfAuthoriser(ZERO);
        yTransData.setSourceId(OFS_SOURCE);
        yTransData.setVersionId(VERSION_COLL_SCREEN);
        yTransData.setTransactionId(concatId);
        transactionData.add(yTransData);
        FfTellerAutoCollScreen.info("transactionData List " + transactionData.toString());
        records.add(ebCollPostRec.toStructure());
        FfTellerAutoCollScreen.info("records List " + records.toString());

    }

}
