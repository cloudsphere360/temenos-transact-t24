package com.temenos.fusion;

import com.temenos.api.LocalRefGroup;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.activityhook.ArrangementContext;
import com.temenos.t24.api.hook.arrangement.ActivityLifecycle;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangementactivity.AaArrangementActivityRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaproductcatalog.AaProductCatalogRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 *
 * @author Balaji JV
 * 
 *         Created: 18-FEB-2026 Atached to : AA.PRD.DES.ACTIVITY.API > LN.API
 *
 */
public class FfNetOffCustValRout extends ActivityLifecycle {

    @Override
    public TValidationResponse validateRecord(AaAccountDetailsRecord accountDetailRecord,
            AaArrangementActivityRecord arrangementActivityRecord, ArrangementContext arrangementContext,
            AaArrangementRecord arrangementRecord, AaArrangementActivityRecord masterActivityRecord,
            TStructure productPropertyRecord, AaProductCatalogRecord productRecord, TStructure curRecord) {

        DataAccess da = new DataAccess(this);
        Session session = new Session(this);
        Contract contractObj = new Contract(this);
        String arrId = arrangementContext.getArrangementId();
        contractObj.setContractId(arrId);
        String companyId = session.getCompanyId();
        String closecus = "";
        String netoffcus = "";
        String repaycus = "";
        CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));

        String finMnemonic = companyObj.getFinancialMne().getValue();

        netoffcus = arrangementActivityRecord.getCustomer().get(0).getCustomer().getValue();

        AaPrdDesAccountRecord desActObj = new AaPrdDesAccountRecord(curRecord);

        for (LocalRefGroup field : desActObj.getLocalRefGroups("FF.CLS.LOAN")) {

            String closevalue = field.getLocalRefField("FF.CLS.LOAN").getValue();

            if (closevalue != null) {

                AaArrangementRecord arrRec = new AaArrangementRecord(
                        da.getRecord(finMnemonic, "AA.ARRANGEMENT", "", closevalue));

                closecus = arrRec.getCustomer().get(0).getCustomer().getValue();

                if (!netoffcus.equals(closecus)) {

                    throw new T24CoreException("", "EB-FF.NETOFF");
                }

            }
        }
        for (LocalRefGroup field : desActObj.getLocalRefGroups("FF.REPAY.LOAN")) {

            String repayvalue = field.getLocalRefField("FF.REPAY.LOAN").getValue();

            if (repayvalue != null) {

                AaArrangementRecord arrRec = new AaArrangementRecord(
                        da.getRecord(finMnemonic, "AA.ARRANGEMENT", "", repayvalue));

                repaycus = arrRec.getCustomer().get(0).getCustomer().getValue();

                if (!netoffcus.equals(repaycus)) {

                    throw new T24CoreException("", "EB-REPAY.FF.NETOFF");
                }

            }
        }

        return desActObj.getValidationResponse();
    }
}
