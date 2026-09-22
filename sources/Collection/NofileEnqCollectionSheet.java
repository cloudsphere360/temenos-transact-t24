package com.temenos.fusion;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.math.BigDecimal;
import com.temenos.api.TDate;
import com.temenos.api.TField;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcentredetail.EbFfCentreDetailRecord;
import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloandetails.FmEntityNumberClass;

/*-----------------------------------------------------------------------------
 * @author Vinothini P
 * Date Created:
 * Attached as : Nofile Enq Routine
 * EB.API : NA
 * Attached to :NA
 * Description: 
 *------------------------------------------------------------------------------ 
 * Modification History :
 *----------------------------------------------------------------------------- 
 *22-Aug-2025   Development      Initial Version
 *-----------------------------------------------------------------------------
 */
public class NofileEnqCollectionSheet extends Enquiry {
    private static final String BOOKING = "BOOKING";
    private static final String EBFFCENTREDETAIL = "EB.FF.CENTRE.DETAIL";

    private static final String L3API = "L3API";
    private static final Logger LOGGER = LoggerFactory.getLogger(L3API);
    private List<String> retvalues = new ArrayList<>();
    String roEmployeeId;
    String branchmanager;
    String demandDate;

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        LOGGER.info("NofileEnqCollectionSheet:setIds routine triggered");

        DataAccess dataAccess = new DataAccess(this);
        Session ssObj = new Session(this);

        CompanyRecord cmpObj = new CompanyRecord(dataAccess.getRecord("COMPANY", ssObj.getCompanyId()));

        String mnemonic = cmpObj.getCustomerMnemonic().getValue();
        String finMnemonic = cmpObj.getFinancialMne().getValue();

        String[] filters = extractFilters(filterCriteria);

        String roEmpId = filters[0];
        String bmEmpId = filters[1];
        demandDate = filters[2];

        List<String> centreList = getCentreList(dataAccess, roEmpId, bmEmpId);

        for (String centreId : centreList) {

            List<String> arrangementList = getArrangements(dataAccess, finMnemonic, centreId);

            EbFfCentreDetailRecord centreRec = new EbFfCentreDetailRecord(
                    dataAccess.getRecord("", EBFFCENTREDETAIL, "", centreId));

            roEmployeeId = centreRec.getCurrentRo().toString();
            branchmanager = centreRec.getBranchManager().toString();

            for (String arrangementId : arrangementList) {

                try {

                    EbFfCollectionDetsRecord collection = new EbFfCollectionDetsRecord(
                            dataAccess.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS", "", arrangementId));

                    processArrangements(collection, mnemonic, finMnemonic, dataAccess, demandDate, arrangementId);

                } catch (Exception e) {

                    LOGGER.error("Error getting collection details", e);
                }
            }
        }

        return retvalues;
    }

    private String[] extractFilters(List<FilterCriteria> filterCriteria) {

        String roEmpId = null;
        String bmEmpId = null;

        for (FilterCriteria criteria : filterCriteria) {

            String field = criteria.getFieldname();
            String value = criteria.getValue();

            if ("RO.EMP.ID".equalsIgnoreCase(field)) {
                roEmpId = value;

            } else if ("BM.EMP.ID".equalsIgnoreCase(field)) {
                bmEmpId = value;

            } else if ("DEMAND.DATE".equalsIgnoreCase(field)) {
                demandDate = value;
            }
        }

        return new String[] { roEmpId, bmEmpId, demandDate };
    }

    private List<String> getCentreList(DataAccess dataAccess, String roEmpId, String bmEmpId) {

        List<String> centreList = new ArrayList<>();

        try {

            if (roEmpId != null && !roEmpId.isEmpty()) {

                centreList = dataAccess.selectRecords("", EBFFCENTREDETAIL, "", "WITH CURRENT.RO EQ " + roEmpId);

            } else if (bmEmpId != null && !bmEmpId.isEmpty()) {

                centreList = dataAccess.selectRecords("", EBFFCENTREDETAIL, "", "WITH BRANCH.MANAGER EQ " + bmEmpId);
            }

        } catch (Exception e) {

            LOGGER.error("Error retrieving centre IDs", e);
        }

        return centreList;
    }

    private List<String> getArrangements(DataAccess dataAccess, String finMnemonic, String centreId) {

        return dataAccess.selectRecords(finMnemonic, "EB.FF.COLLECTION.DETS", "",
                "WITH CENTRE EQ " + centreId + " AND DUE.DATE EQ " + demandDate);
    }

    public List<String> processArrangements(EbFfCollectionDetsRecord collection, String mnemonic, String finMnemonic,
            DataAccess dataAccess, String demandDate, String arrangementId) {
        Session session = new Session(this);
        Contract contract = new Contract(this);

        try {
            String loanAccountNumber = arrangementId;
            String branchName = null;
            String centreName = null;
            String groupName = null;
            String guarantorId = null;
            String parkedAmount = null;
            String loanDPD = null;
            String collectionOfficerName = null;
            String collectionOfficerId = null;
            String customerDPD = null;
            String customerName = null;
            String loanStatus = null;

            String centreId = collection.getCentre().toString();
            String branchId = collection.getBranch().toString();
            String customerId = collection.getCustomer().toString();
            String groupId = collection.getGroup().toString();
            String villageId = collection.getVillage().toString();
            BigDecimal dueInterestAmt = BigDecimal.ZERO;
            BigDecimal duePrincipalAmt = BigDecimal.ZERO;
            BigDecimal dueTotalAmt = BigDecimal.ZERO;
            for (int i = 0; i < collection.getDueDate().size(); i++) {
                String dueDate = collection.getDueDate().get(i).getValue();
                if (demandDate.equals(dueDate)) {
                    dueInterestAmt = new BigDecimal(collection.getInterestAmt().get(i).getValue());
                    duePrincipalAmt = new BigDecimal(collection.getPrincipalAmt().get(i).getValue());
                    dueTotalAmt = new BigDecimal(collection.getTotalDue().get(i).getValue());
                    break;
                }
            }

            String dueInterest = dueInterestAmt.toString();
            String duePrincipal = duePrincipalAmt.toString();
            String emiAmount = dueTotalAmt.toString();
            // Next due date
            String nextDemandDate = getNextDemandDate(collection);
            LOGGER.info("nextDemandDate " + nextDemandDate);
            contract.setContractId(arrangementId);
            LOGGER.info("finMnemonic " + finMnemonic);
            loanStatus = getloanStatus(dataAccess, finMnemonic, arrangementId);

            TDate today = new TDate(session.getCurrentVariable("!TODAY"));
            // Tenure
            String loanTenure = getloanTenure(contract, today);

            // Balances
            String overduePrincipal = getBalance(contract, "NABACCOUNT", BOOKING);
            String overdueInterest = getBalance(contract, "NABINTEREST", BOOKING);
            String currAccount = getBalance(contract, "CURACCOUNT", BOOKING);
            String dueAccount = getBalance(contract, "DUEACCOUNT", BOOKING);
            parkedAmount = getBalance(contract, "UNCACCOUNT", BOOKING);
            LOGGER.info("overduePrincipal " + overduePrincipal);
            LOGGER.info("overdueInterest " + overdueInterest);
            LOGGER.info("currAccount " + currAccount);
            LOGGER.info("dueAccount " + dueAccount);
            LOGGER.info("parkedAmount " + parkedAmount);
            String principalOutstanding;
            if (demandDate.equalsIgnoreCase(today.toString())) {
                principalOutstanding = new BigDecimal(currAccount).add(new BigDecimal(dueAccount)).toString();
            } else {
                principalOutstanding = currAccount;
            }
            LOGGER.info("principalOutstanding " + principalOutstanding);
            String totalEmiPending = new BigDecimal(overduePrincipal).add(new BigDecimal(overdueInterest)).toString();
            customerName = getCustomerName(mnemonic, dataAccess, customerId);

            String guarantorName = getGuarantorName(arrangementId, finMnemonic, dataAccess);

            AaAccountDetailsRecord accountDetails = new AaAccountDetailsRecord(
                    dataAccess.getRecord(finMnemonic, "AA.ACCOUNT.DETAILS", "", arrangementId));
            String paymentStatus = accountDetails.getArrAgeStatus().toString();

            // Build row
            List<String> row = new ArrayList<>();
            row.add(roEmployeeId);
            row.add(branchmanager);
            row.add(centreId);
            row.add(branchId);
            row.add(customerId);
            row.add(dueInterest);
            row.add(duePrincipal);
            row.add(loanAccountNumber);
            row.add(loanStatus);
            row.add(principalOutstanding);
            row.add(villageId);
            row.add(totalEmiPending);
            row.add(centreName);
            row.add(customerName);
            row.add(demandDate);
            row.add(groupId);
            row.add(groupName);
            row.add(guarantorId);
            row.add(guarantorName);
            row.add(loanTenure);
            row.add(branchName);
            row.add(emiAmount);
            row.add(nextDemandDate);
            row.add(parkedAmount);
            row.add(paymentStatus);
            row.add(loanDPD);
            row.add(collectionOfficerName);
            row.add(collectionOfficerId);
            row.add(customerDPD);
            retvalues.add(String.join("*", row));

        } catch (Exception e) {
            LOGGER.error("Error processing arrangement: " + arrangementId, e);
        }
        return retvalues;
    }

    private String getCustomerName(String mnemonic, DataAccess dataAccess, String customerId) {
        String customerName = null;
        try {
            CustomerRecord customer = new CustomerRecord(dataAccess.getRecord(mnemonic, "CUSTOMER", "", customerId));
            customerName = customer.getShortName(0).getValue();
        } catch (Exception e) {
            LOGGER.error("unable to record customer record : " + e.getMessage());
        }
        return customerName;
    }

    private String getNextDemandDate(EbFfCollectionDetsRecord collection) {

        String nextDemandDate = null;

        for (TField due : collection.getDueDate()) {

            String val = due.getValue();

            if (val.compareTo(demandDate) > 0 && (nextDemandDate == null || val.compareTo(nextDemandDate) < 0)) {

                nextDemandDate = val;
            }
        }

        return nextDemandDate;

    }

    private String getloanTenure(Contract contract, TDate today) {
        String loanTenure = null;
        try {

            AaPrdDesTermAmountRecord termRec = new AaPrdDesTermAmountRecord(
                    contract.getConditionForPropertyEffectiveDate("COMMITMENT", today));
            LOGGER.info("termRec " + termRec.toString());
            loanTenure = termRec.getTerm().getValue();
        } catch (Exception e) {
            LOGGER.error("Error getting tenure" + e.getMessage());
        }
        return loanTenure;
    }

    private String getloanStatus(DataAccess dataAccess, String finMnemonic, String arrangementId) {
        String loanStatus = null;

        try {
            AaArrangementRecord arrangement = new AaArrangementRecord(
                    dataAccess.getRecord(finMnemonic, "AA.ARRANGEMENT", "", arrangementId));
            loanStatus = arrangement.getArrStatus().toString();

        } catch (Exception e) {
            LOGGER.error("Error getting arrangement " + arrangementId + ": " + e.getMessage());
        }
        return loanStatus;
    }

    private String getGuarantorName(String arrangementId, String finMnemonic, DataAccess dataAccess) {
        try {
            EbFfLoanDetailsRecord loanDetails = new EbFfLoanDetailsRecord(
                    dataAccess.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", arrangementId));
            List<FmEntityNumberClass> relationList = loanDetails.getFmEntityNumber();
            for (FmEntityNumberClass rel : relationList) {

                if ("YES".equalsIgnoreCase(rel.getIsGuarantor().getValue())) {
                    return rel.getLegalName().getValue();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error retrieving guarantor name for " + arrangementId + ": " + e.getMessage());
        }
        return "";
    }

    private String getBalance(Contract contract, String accountType, String bookingType) {
        List<BalanceMovement> movements = contract.getContractBalanceMovements(accountType, bookingType);
        return (movements != null && !movements.isEmpty()) ? movements.get(0).getBalance().toString() : "0";
    }
}
