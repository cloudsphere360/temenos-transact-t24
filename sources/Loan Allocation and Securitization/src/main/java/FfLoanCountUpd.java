package com.temenos.fusion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.temenos.api.TStructure;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aacustomerarrangement.AaCustomerArrangementRecord;
import com.temenos.t24.api.records.aacustomerarrangement.ArrangementClass;
import com.temenos.t24.api.records.aacustomerarrangement.ProductLineClass;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.ebfffunderdetails.EbFfFunderDetailsRecord;
import com.temenos.t24.api.records.ebfffunderdetails.FfFunderNameTrancheClass;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author sr115630
 *
 */
public class FfLoanCountUpd extends ServiceLifecycle {
    List<String> arrList = new ArrayList<>();
    Map<String, Integer> funderCountMap = new HashMap<>();
    String arrangementId = "";
    String finMnemonic = "";
    String productValue = "";
    DataAccess da = new DataAccess(this);
    String arrId = "";
    private static final FusionFileLogger FF_LOAN_COUNT_UPD = FusionFileLogger.getLogger(FfLoanCountUpd.class);
    Contract contract = new Contract(this);

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        try {

            arrList = da.selectRecords(finMnemonic, "AA.CUSTOMER.ARRANGEMENT", "", "");
            FF_LOAN_COUNT_UPD.info("arrList" + arrList);
        } catch (Exception e) {
            e.getMessage();

        }
        return arrList;
    }

    @Override
    public void postUpdateRequest(String id, ServiceData serviceData, String controlItem,
            List<TransactionData> transactionData, List<TStructure> records) {
        try {
            EbFfFunderDetailsRecord ebFfFunderRec = new EbFfFunderDetailsRecord(
                    da.getRecord("EB.FF.FUNDER.DETAILS", id));
            FF_LOAN_COUNT_UPD.info("ebFfFunderRec" + ebFfFunderRec);
            Set<String> funderSet = new HashSet<>();
            for (FfFunderNameTrancheClass funder : ebFfFunderRec.getFfFunderNameTranche()) {
                if ((funder != null) && (!funder.getFfFunderNameTranche().getValue().isEmpty())) {
                    funderSet.add(funder.getFfFunderNameTranche().getValue());
                }
            }
            FF_LOAN_COUNT_UPD.info("funderSet" + funderSet);
            AaCustomerArrangementRecord aaRec = new AaCustomerArrangementRecord(
                    da.getRecord("AA.CUSTOMER.ARRANGEMENT", id));
            FF_LOAN_COUNT_UPD.info("aaRec" + aaRec);
            for (ProductLineClass product : aaRec.getProductLine()) {
                productValue = product.getProductLine().getValue();
                FF_LOAN_COUNT_UPD.info("productValue" + productValue);
                if (productValue.equals("LENDING")) {
                    for (ArrangementClass arrangemnt : product.getArrangement()) {
                        arrangementId = arrangemnt.getArrangement().getValue();

                        contract.setContractId(arrangementId);

                        AaPrdDesAccountRecord aaPrdDesAccountRecord = new AaPrdDesAccountRecord(
                                contract.getConditionForProperty("ACCOUNT"));
                        String funderTranche = aaPrdDesAccountRecord.getLocalRefField("FF.FN.TRANCHE").getValue();
                        FF_LOAN_COUNT_UPD.info("funderTranche" + funderTranche);

                        if (funderSet.contains(funderTranche)) {
                            funderCountMap.put(funderTranche, funderCountMap.getOrDefault(funderTranche, 0) + 1);
                            FF_LOAN_COUNT_UPD.info("funderCountMap" + funderCountMap);
                        }

                    }
                }

            }
            FF_LOAN_COUNT_UPD.info("productValue" + productValue);
            FF_LOAN_COUNT_UPD.info("arrangementId" + arrangementId);

            EbFfFunderDetailsRecord newEbFffunderRecs = new EbFfFunderDetailsRecord();
            FF_LOAN_COUNT_UPD.info("Entering try block");
            FF_LOAN_COUNT_UPD.info("Created ebFffunderRecs");
            List<String> trancheList = new ArrayList<>(funderCountMap.keySet());
            FF_LOAN_COUNT_UPD.info("trancheList = " + trancheList);

            for (String trancheValue : trancheList) {
                String tracheCount = String.valueOf(funderCountMap.get(trancheValue));
                FF_LOAN_COUNT_UPD.info("tracheCount" + tracheCount);
                for (FfFunderNameTrancheClass funders : ebFfFunderRec.getFfFunderNameTranche()) {
                    String curTrancheValue = funders.getFfFunderNameTranche().getValue();
                    FF_LOAN_COUNT_UPD.info("Inside funders loop");
                    FF_LOAN_COUNT_UPD.info("funders = " + funders);
                    FF_LOAN_COUNT_UPD.info("curTrancheValue = " + curTrancheValue);
                    if (curTrancheValue.equals(trancheValue))
                        funders.getFfLoanCount().set(tracheCount);
                    FF_LOAN_COUNT_UPD.info("Loan count after update = " + funders.getFfLoanCount().getValue());
                    FF_LOAN_COUNT_UPD.info("ebFffunderRecs" + newEbFffunderRecs);
                    FF_LOAN_COUNT_UPD.info("Match found. Setting loan count = " + tracheCount);
                }
            }
              TransactionData txnValue = new TransactionData();
              FF_LOAN_COUNT_UPD.info("Before TransactionData creation");
              txnValue.setVersionId("EB.FF.FUNDER.DETAILS,OFS.FUNDER.CUSTOMER");
              txnValue.setTransactionId(id);
              txnValue.setFunction("INPUT");
              transactionData.add(txnValue);
              txnValue.setSourceId("FF.FUNDER.PROCESS");
              records.add(ebFfFunderRec.toStructure());
              FF_LOAN_COUNT_UPD.info("Record added successfully");
             
        } catch (Exception e) {
            FF_LOAN_COUNT_UPD.error("Exception occurred", e);
        }

    }

}
