package com.temenos.fusion;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.dates.DatesRecord;
import com.temenos.t24.api.records.imdocumentimage.ImDocumentImageRecord;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.system.Session;

/**
 * @author jh116454
 * @Attached To: VERSION > IM.DOCUMENT.IMAGE,PETTY.CASH.IMG         
 * @Attached As: IMAGE.REFERENCE - Auto New Content > EB.API > FF.PETTY.IMG.REF.UPD
 * @Description: defaulting the Image ID in the version
 * 
 */

public class FfPettyImgRefUpd extends RecordLifecycle {
    private static final FusionFileLogger yPettyImgRefUpdLog = FusionFileLogger.getLogger(FfPettyImgRefUpd.class);

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        ImDocumentImageRecord yImDocImgRec = new ImDocumentImageRecord(currentRecord);
        Session ySession = new Session(this);
        String companycode = ySession.getCompanyId();
        Date yDate = new Date(this);
        DatesRecord yDateRec = yDate.getDates();
        String yTodDt = yDateRec.getToday().getValue();
        String yPettyImgID = companycode + "-" + yTodDt;
        yPettyImgRefUpdLog.info("Petty ID -> " + yPettyImgID);
        yImDocImgRec.setImageReference(yPettyImgID);
        currentRecord.set(yImDocImgRec.toStructure());
    }

}
