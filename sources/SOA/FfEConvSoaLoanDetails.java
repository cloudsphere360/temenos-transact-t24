package com.temenos.fusion;

import java.util.Arrays;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;

import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.aa.contractapi.BalanceMovement;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddesaccount.AltIdTypeClass;
import com.temenos.t24.api.records.aaprddesinterest.AaPrdDesInterestRecord;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcollectiondets.EbFfCollectionDetsRecord;
import com.temenos.t24.api.records.ebffcollectiondetshistory.EbFfCollectionDetsHistoryRecord;
import com.temenos.t24.api.records.ebffloandetails.AddressTypeClass;
import com.temenos.t24.api.records.ebffloandetails.EbFfLoanDetailsRecord;
import com.temenos.t24.api.records.ebffloanpaymenthis.EbFfLoanPaymentHisRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfEConvSoaLoanDetails extends Enquiry {
    DataAccess da = new DataAccess(this);

    String retValues = "";
    String arrangementId = "";
    String finMnemonic = "";
    String mnemonic = "";
    String arrStdt = "";

    String contractNo = "";
    String customerNo = "";
    String customerName = "";
    String permanentAddress = "";
    String telephoneNo = "";
    String branch = "";
    String accountNo = "";
    String loanOpeningDate = "";
    String interestRate = "";
    String tenure = "";
    String loanAmount = "";
    String firstInstallmentDate = "";
    String lastInstallmentDate = "";
    String parkedAmount = "";
    String loanSanctionDate = "";
    String altId = "";

    boolean migratedContractFlg = false;

    @Override
    public String setValue(String value, String currentId, TStructure currentRecord,
            List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        try {
            arrangementId = value;
            Contract contract = new Contract(this);
            Session session = new Session(this);
            String companyId = session.getCompanyId();
            initialiseCompanyInfo(companyId);

            contract.setContractId(arrangementId);
            getArrangementDetails(contract);
            getCustomerDetails(customerNo);
            getEbFfLoanDetails(arrangementId);
            getAaArrInterestDetails(contract);
            getEbFfCollectionDets(arrangementId);
            getEbFfLoanPaymentHisDets(arrangementId);
            getEcbDetails(contract);
            getAaArrAccountDetails(contract);
            retValues = contractNo + "*" + customerNo + "*" + customerName + "*" + permanentAddress + "*" + telephoneNo
                    + "*" + branch + "*" + accountNo + "*" + loanOpeningDate + "*" + interestRate + "*" + tenure + "*"
                    + loanAmount + "*" + firstInstallmentDate + "*" + lastInstallmentDate + "*" + parkedAmount + "*"
                    + loanSanctionDate + "*" + altId;
        } catch (Exception e) {
            e.getMessage();
        }
        return retValues;
    }

    public void initialiseCompanyInfo(String companyId) {
        try {
            CompanyRecord companyObj = new CompanyRecord(da.getRecord("COMPANY", companyId));
            finMnemonic = companyObj.getFinancialMne().getValue();
            mnemonic = companyObj.getCustomerMnemonic().getValue();
            branch = companyObj.getCompanyName().get(0).getValue();
            String[] branchNamePart = branch.split("-");
            branch = branchNamePart[0];
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getArrangementDetails(Contract contract) {
        try {
            AaArrangementRecord arrRec = contract.getContract();
            contractNo = arrangementId;
            accountNo = contractNo;
            arrStdt = arrRec.getStartDate().getValue();
            customerNo = arrRec.getCustomer().get(0).getCustomer().getValue();
            tenure = contract.getTerm();
            loanAmount = contract.getTermAmount().get();
            if (arrRec.getOrigContractDate().getValue() != null && !arrRec.getOrigContractDate().getValue().isEmpty()) {
                migratedContractFlg = true;
                loanOpeningDate = arrRec.getOrigContractDate().getValue();
            } else {
                loanOpeningDate = arrRec.getStartDate().getValue();
            }
            loanSanctionDate = loanOpeningDate;
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getCustomerDetails(String customerNumber) {
        StringBuilder custName = new StringBuilder();
        try {
            CustomerRecord cusRec = new CustomerRecord(da.getRecord(mnemonic, "CUSTOMER", "", customerNumber));

            TField name1Field = (cusRec.getName1() != null && !cusRec.getName1().isEmpty()) ? cusRec.getName1().get(0)
                    : null;

            TField name2Field = (cusRec.getName2() != null && !cusRec.getName2().isEmpty()) ? cusRec.getName2().get(0)
                    : null;

            List<String> cusNameVal = Arrays.asList(checkFiled(name1Field), checkFiled(name2Field),
                    checkFiled(cusRec.getFamilyName()));
            for (String cusNameValList : cusNameVal) {
                cusNameAppendIfNotEmpty(custName, cusNameValList);
            }
            customerName = custName.toString();

            telephoneNo = cusRec.getPhone1().get(0).getPhone1().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void cusNameAppendIfNotEmpty(StringBuilder custName, String value) {
        if (value != null && !value.isEmpty()) {
            if (custName.length() > 0) {
                custName.append(" ");
            }
            custName.append(value);
        }
    }

    public void getEbFfLoanDetails(String arrangementId) {
        StringBuilder address = new StringBuilder();
        try {
            EbFfLoanDetailsRecord ffLoanDetsRec = new EbFfLoanDetailsRecord(
                    da.getRecord(finMnemonic, "EB.FF.LOAN.DETAILS", "", arrangementId));
            for (AddressTypeClass addrList : ffLoanDetsRec.getAddressType()) {
                if (addrList.getAddressType().getValue().equalsIgnoreCase("CURRENT")) {
                    List<String> addrVal = Arrays.asList(checkFiled(addrList.getAddress1()),
                            checkFiled(addrList.getAddress2()), checkFiled(addrList.getCity()),
                            checkFiled(addrList.getTehsil()), checkFiled(addrList.getVillageName()),
                            checkFiled(addrList.getDistrictName()), checkFiled(addrList.getStateName()),
                            checkFiled(addrList.getPincode()));
                    for (String addrValList : addrVal) {
                        addressAppendIfNotEmpty(address, addrValList);
                    }
                    permanentAddress = address.toString();
                    return;
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public String checkFiled(TField field) {
        try {
            return (field != null) ? field.getValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void addressAppendIfNotEmpty(StringBuilder address, String value) {
        if (value != null && !value.isEmpty()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(value);
        }
    }

    public void getAaArrAccountDetails(Contract contract) {
        try {
            if (migratedContractFlg) {
                AaPrdDesAccountRecord aaArrAccRec = new AaPrdDesAccountRecord(
                        contract.getConditionForProperty("ACCOUNT"));
                for (AltIdTypeClass altType : aaArrAccRec.getAltIdType()) {
                    if (altType.getAltIdType().getValue().equals("LEGACY")) {
                        altId = altType.getAltId().getValue();
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getAaArrInterestDetails(Contract contract) {
        try {
            AaPrdDesInterestRecord aaArrIntRec = new AaPrdDesInterestRecord(
                    contract.getConditionForProperty("PRINTEREST"));
            interestRate = aaArrIntRec.getFixedRate(0).getFixedRate().getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getEbFfCollectionDets(String arrId) {
        if (migratedContractFlg) {
            migratedContractCollDets(arrId);
        } else {
            nonMigratedContractCollDets(arrId);
        }
    }

    public void migratedContractCollDets(String arrId) {
        try {
            String ebFfCollDetsHistRecId = arrId + "-" + arrStdt + ".01";
            EbFfCollectionDetsHistoryRecord ebFfCollDetsHistRec = new EbFfCollectionDetsHistoryRecord(
                    da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS.HISTORY", "", ebFfCollDetsHistRecId));

            if (ebFfCollDetsHistRec.toString().isEmpty()) {
                return;
            }

            processCollectionDetails(ebFfCollDetsHistRec.getDueDate(), true);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void nonMigratedContractCollDets(String arrId) {
        try {
            EbFfCollectionDetsRecord ebFfCollDetsRec = new EbFfCollectionDetsRecord(
                    da.getRecord(finMnemonic, "EB.FF.COLLECTION.DETS", "", arrId));

            if (ebFfCollDetsRec.toString().isEmpty()) {
                return;
            }

            processCollectionDetails(ebFfCollDetsRec.getDueDate(), false);
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void processCollectionDetails(List<TField> dueDates, boolean migrated) {
        try {
            int startIndex = migrated ? 0 : 1;
            firstInstallmentDate = dueDates.get(startIndex).getValue();
            lastInstallmentDate = dueDates.get(dueDates.size() - 1).getValue();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void getEbFfLoanPaymentHisDets(String arrId) {
        try {
            if (migratedContractFlg) {
                EbFfLoanPaymentHisRecord ffLoanPaymentHisRec = new EbFfLoanPaymentHisRecord(
                        da.getRecord("", "EB.FF.LOAN.PAYMENT.HIS", "", arrId));
                tenure = ffLoanPaymentHisRec.getLoanTenure().getValue() + " "
                        + ffLoanPaymentHisRec.getTenureUnit().getValue();
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void getEcbDetails(Contract contract) {
        try {
            parkedAmount = getBalance(contract, "UNCACCOUNT", "BOOKING");
        } catch (NumberFormatException e) {
            e.getMessage();
        }
    }

    public String getBalance(Contract contract, String accountType, String bookingType) {
        List<BalanceMovement> movements = contract.getContractBalanceMovements(accountType, bookingType);
        if (movements == null || movements.isEmpty()) {
            return "0.00";
        }
        String balance = String.valueOf(movements.get(0).getBalance());
        return (balance == null || balance.isEmpty()) ? "0.00" : balance;
    }

}
