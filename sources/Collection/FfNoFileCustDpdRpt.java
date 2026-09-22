package com.temenos.fusion;

import java.util.ArrayList;
import java.util.List;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebffcustdpd.DateClass;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebffcustdpd.EbFfCustDpdRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

//* ENQUIRY>FF.CUST.DPD.DETAILS
//* SS>NOFILE.FF.CUST.DPD
//* EB.API>FF.NOFILE.CUST.DPD.RPT
public class FfNoFileCustDpdRpt extends Enquiry {
    
    // ===== Logging =====
    private static final boolean DEBUG = true;
    private final DataAccess da = new DataAccess(this);
    private final Session session = new Session();

    // Base fields
    String loanId = "";
    String customerName = "";
    String date = "";
    String currDpd = "";
    String curDpdRef = "";
    String dpdStatus = "";
    String peakDpd = "";
    String peadDpdRef = "";
    String peakCollDate = "";
    String custLevelFlag = "";
    String dateFlagset = "";
    String dateFlagRel = "";
    String modifiedCustomerNo = "";
    //Filter fields
    String fCustId = "";
    String opCustId = "";
    String fCustNo = "";
    String opCustNo = "";
    String fCustName = "";
    String opCustName = "";
    String fDateEq = "";
    String opDate = "";
    String fDateFrom = "";
    String fDateTo = "";
    String fCurDpd = "";
    String opCurDpd = "";
    String fDpdStatus = "";
    String opDpdStatus = "";
    String fPeakDpd = "";
    String opPeakDpd = "";
    String fPeakDpdRef = "";
    String opPeakDpdRef = "";
    String fCurDpdAcRef = "";
    String opCurDpdAcRef = "";
    String fCusFlag = "";
    String opCusFlag = "";
    String fFreezeDpd = "";
    String opFreezeDpd = "";
    String fDateOfFreeze = "";
    String opDateOfFreeze = "";
    String fFreezeEndDt = "";
    String opFreezeEndDt = "";
    String fDateFlagRel = "";
    String opDateFlagRel = "";
    String fDateFlagRelFrom = "";
    String fDateFlagRelTo = "";

    // ============================
    // Helper: safe & normalization
    // ============================
    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean isYYYYMMDD(String s) {
        return s != null && s.matches("\\d{8}");
    }

    private static int ymdCompare(String a, String b) {
        return a.compareTo(b);
    }

    // ============================
    // Helper: operand checks
    // (Supports numeric codes & text. Adjust numeric codes if your site differs.)
    // ============================
    private static boolean isEQ(String op) {
        op = safe(op).toUpperCase();
        return op.equals("1") || op.equals("EQ");
    }

    private static boolean isRG(String op) {
        op = safe(op).toUpperCase();
        return op.equals("2") || op.equals("RG");
    }

    private static boolean isLT(String op) {
        op = safe(op).toUpperCase();
        return op.equals("3") || op.equals("LT");
    }

    private static boolean isGT(String op) {
        op = safe(op).toUpperCase();
        return op.equals("4") || op.equals("GT");
    }

    // If your site uses different numeric codes for LE / GE, adjust here
    private static boolean isLE(String op) {
        op = safe(op).toUpperCase();
        return op.equals("8") || op.equals("LE");
    }

    private static boolean isGE(String op) {
        op = safe(op).toUpperCase();
        return op.equals("9") || op.equals("GE");
    }

    // Contains (CT) code differs per site; include common cases + "CT"
    private static boolean isCT(String op) {
        op = safe(op).toUpperCase();
        return op.equals("6") || op.equals("CT");
    }

    // ============================
    // Comparators
    // ============================
    private boolean matchNumber(String actual, String filterVal, String op) {
        if (isEmpty(filterVal))
            return true;
        if (isEmpty(actual))
            return false;

        double a, b;
        try {
            a = Double.parseDouble(actual.trim());
            b = Double.parseDouble(filterVal.trim());
        } catch (Exception e) {
            return false;
        }

        if (isEQ(op))
            return Double.compare(a, b) == 0;
        if (isLT(op))
            return a < b;
        if (isGT(op))
            return a > b;
        if (isLE(op))
            return a <= b;
        if (isGE(op))
            return a >= b;
        return true; // unknown op -> allow
    }

    private boolean matchText(String actual, String filterVal, String op) {
        if (isEmpty(filterVal))
            return true;
        actual = safe(actual);
       // String f = safe(filterVal);
        String f = safe(filterVal).trim();
        f = f.replaceAll("^\\.+|\\.+$", "");
        if (isEQ(op))
            return actual.equals(f);
        if (isCT(op))
            return actual.toUpperCase().contains(f.toUpperCase());
        return true; // unknown op -> allow
    }

    /**
     * DATE / DT.OF.FLAG.RELEASE matching (strict when a filter is present): - If no
     * filter provided at all (op, eqVal, from, to all empty) => return true (do not
     * filter). - EQ => requires eqVal "YYYYMMDD" and actual must be valid & equal.
     * - RG => requires from/to "YYYYMMDD" and actual must be valid & within
     * [from,to].
     */
    private boolean matchDateField(String actual, String eqVal, String from, String to, String op) {
        // If no filter provided for this field, do not filter the row.
        if (isEmpty(op) && isEmpty(eqVal) && isEmpty(from) && isEmpty(to)) {
            return true;
        }

        actual = safe(actual);
        if (!isYYYYMMDD(actual))
            return false; // we only validate actual if a filter exists

        String opNorm = safe(op).toUpperCase();

        // Auto-detect if op missing but values supplied
        if (opNorm.isEmpty()) {
            if (!isEmpty(from) && !isEmpty(to)) {
                opNorm = "RG";
            } else if (!isEmpty(eqVal)) {
                opNorm = "EQ";
            }
        }

        if (isEQ(opNorm)) {
            eqVal = safe(eqVal);
            if (!isYYYYMMDD(eqVal))
                return false;
            return actual.equals(eqVal);
        }

        if (isRG(opNorm)) {
            from = safe(from);
            to = safe(to);
            if (!isYYYYMMDD(from) || !isYYYYMMDD(to))
                return false;
            return ymdCompare(actual, from) >= 0 && ymdCompare(actual, to) <= 0;
        }

        // Unspecified/unknown op with some values -> be permissive
        return true;
    }

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        List<String> returnValues = new ArrayList<>();

        // Read filters
        for (FilterCriteria filter : filterCriteria) {
            String field = safe(filter.getFieldname()).toUpperCase();
            String value = safe(filter.getValue());
            String op = safe(filter.getOperand());

            switch (field) {
            
            case "@ID":
                fCustId = value;
                opCustId = op;
                break;
                
            case "DATE":
                opDate = op;
                if (isEQ(op)) {
                    fDateEq = value;
                } else if (isRG(op)) {
                    String[] d = value.split("\\s+");
                    if (d.length > 0)
                        fDateFrom = d[0];
                    if (d.length > 1)
                        fDateTo = d[1];
                } else {
                    // Operator missing? auto-detect by tokens
                    String[] d = value.split("\\s+");
                    if (d.length == 1) {
                        opDate = "EQ";
                        fDateEq = d[0];
                    } else if (d.length >= 2) {
                        opDate = "RG";
                        fDateFrom = d[0];
                        fDateTo = d[1];
                    }
                }
                break;

            case "CUR.DPD":
                fCurDpd = value;
                opCurDpd = op;
                break;

            case "DPD.STATUS":
                fDpdStatus = value;
                opDpdStatus = op;
                break;

            case "CUR.DPD.AC.REF":
                fCurDpdAcRef = value;
                opCurDpdAcRef = op;
                break;

            case "PEAK.DPD":
                fPeakDpd = value;
                opPeakDpd = op;
                break;

            case "PEAK.DPD.AC.REF":
                fPeakDpdRef = value;
                opPeakDpdRef = op;
                break;

            case "CUST.LEVEL.FLAG":
                fCusFlag = value;
                opCusFlag = op;
                break;

            case "DT.OF.FLAG.RELEASE":
                opDateFlagRel = op;
                if (isEQ(op)) {
                    fDateFlagRel = value;
                } else if (isRG(op)) {
                    String[] d2 = value.split("\\s+");
                    if (d2.length > 0)
                        fDateFlagRelFrom = d2[0];
                    if (d2.length > 1)
                        fDateFlagRelTo = d2[1];
                } else {
                    // Auto-detect if missing
                    String[] d2 = value.split("\\s+");
                    if (d2.length == 1) {
                        opDateFlagRel = "EQ";
                        fDateFlagRel = d2[0];
                    } else if (d2.length >= 2) {
                        opDateFlagRel = "RG";
                        fDateFlagRelFrom = d2[0];
                        fDateFlagRelTo = d2[1];
                    }
                }
                break;

            case "FREEZE.DPD":
                fFreezeDpd = value;
                opFreezeDpd = op;
                break;

            case "DATE.OF.FREEZE":
                fDateOfFreeze = value;
                opDateOfFreeze = op;
                break;

            case "FREEZE.END.DATE":
                fFreezeEndDt = value;
                opFreezeEndDt = op;
                break;

            default:
                break;
            }
        }

        String companyId = session.getCompanyId();
        CompanyRecord companyRecord = new CompanyRecord(da.getRecord("COMPANY", companyId));
        String finmnemonic = companyRecord.getFinancialMne().getValue();

        List<String> dpdlist;
        try {
            dpdlist = da.selectRecords("", "EB.FF.CUST.DPD", "", "");
            
        } catch (Exception e) {
            dpdlist = new ArrayList<>();
        }

        if (dpdlist.isEmpty()) {
            // LOGG.info("[END DPD ENQUIRY DEBUG] -> No records in EB.FF.CUST.DPD");
            return returnValues;
        }

        // Global counters for reasons
        
        int totalRowsVisited = 0;
        int totalRowsMatched = 0;
        int failDate = 0, failCurDpd = 0, failCurDpdAcRef = 0, failDpdStatus = 0, failPeakDpd = 0, failPeakRef = 0,
                faildateFlagRel = 0;
        int skipLoanByCustId = 0, skipLoanByCustNo = 0, skipLoanByCustName = 0;
        String cusShortName = "";
        for (String dpdlistid : dpdlist) {
            // Record-level: @ID
            if (!matchText(dpdlistid, fCustId, opCustId)) {
                skipLoanByCustId++;
                if (DEBUG) // LOGG.info("Skip loan by LOAN.ID -> " + dpdlistid);
                    continue;
            }

            // Load DPD rows
            EbFfCustDpdRecord dpdRecord = new EbFfCustDpdRecord(
                    da.getRecord("", "EB.FF.CUST.DPD", "", dpdlistid));

            List<DateClass> dateList = dpdRecord.getDate();
            if (dateList == null || dateList.isEmpty()) {
                if (DEBUG) // LOGG.info("Loan has no DATE MV -> " + dpdlistid);
                    continue;
            }

            int i = 0;

            if (DEBUG) {
                // Log a quick min/max/first few dates to see what’s present
                String minD = null, maxD = null;
                int count = 0;
                for (DateClass d : dateList) {
                    String dd = safe(d.getDate().getValue());
                    if (!isYYYYMMDD(dd))
                        continue;
                    if (minD == null || dd.compareTo(minD) < 0)
                        minD = dd;
                    if (maxD == null || dd.compareTo(maxD) > 0)
                        maxD = dd;
                    if (count < 2) // LOGG.info("Sample date row =" + dpdlistid + " date=" + dd);
                        count++;
                }
                
            }
            String customerNo = dpdlistid.split("-")[0];// CUS115693556-NOV2025
            if (customerNo != null && customerNo.length() > 3) {
                modifiedCustomerNo = customerNo.substring(3);
            
            }

            try {
                CustomerRecord cusRecord = new CustomerRecord(da.getRecord("CUSTOMER", modifiedCustomerNo));
                cusShortName = cusRecord.getShortName(0).getValue();
            } catch (Exception e) {

            }
            for (DateClass dates : dateList) {
                totalRowsVisited++;

                date = safe(dates.getDate().getValue());
                currDpd = safe(dates.getCurDpd().getValue());
                curDpdRef = safe(dates.getCurDpdAcRef().getValue());
                dpdStatus = safe(dates.getDpdStatus().getValue());
                peakDpd = safe(dates.getPeakDpd().getValue());
                peadDpdRef = safe(dates.getPeakDpdAcRef().getValue());
                custLevelFlag = safe(dates.getCustLevelFlag().getValue());
                dateFlagset = safe(dates.getDtOfFlagset().getValue());
                dateFlagRel = safe(dates.getDtOfFlagRelease().getValue());
                
                // Row-level filters with reason tracking
                boolean dateOk = matchDateField(date, fDateEq, fDateFrom, fDateTo, opDate);
                
                boolean curOk = matchNumber(currDpd, fCurDpd, opCurDpd);

                boolean curDpdRefOk = matchText(curDpdRef, fCurDpdAcRef, opCurDpdAcRef);

                boolean dpdStatOk = matchText(dpdStatus, fDpdStatus, opDpdStatus);

                boolean peakOk = matchNumber(peakDpd, fPeakDpd, opPeakDpd);

                boolean peakRefOk = matchText(peadDpdRef, fPeakDpdRef, opPeakDpdRef);

                boolean dateFlagRelOk = matchDateField(dateFlagRel, fDateFlagRel, fDateFlagRelFrom, fDateFlagRelTo,
                        opDateFlagRel);
                

                if (!(dateOk && curOk && curDpdRefOk && dpdStatOk && peakOk && peakRefOk && dateFlagRelOk)) {
                    if (DEBUG) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Row FAIL loan=").append(dpdlistid).append(" date=").append(date).append(" [");
                        if (!dateOk) {
                            sb.append("DATE ");
                            failDate++;
                        }
                        if (!curOk) {
                            sb.append("CUR.DPD ");
                            failCurDpd++;
                        }
                        if (!curDpdRefOk) {
                            sb.append("CUR.DPD.AC.REF ");
                            failCurDpdAcRef++;
                        }
                        if (!dpdStatOk) {
                            sb.append("DPD.STATUS ");
                            failDpdStatus++;
                        }
                        if (!peakOk) {
                            sb.append("PEAK.DPD ");
                            failPeakDpd++;
                        }
                        if (!peakRefOk) {
                            sb.append("PEAK.DPD.REF ");
                            failPeakRef++;
                        }
                        if (!dateFlagRelOk) {
                            sb.append("DATE.FLAG.REL ");
                            faildateFlagRel++;
                        }
                        sb.append("]");
                        
                    }
                    continue;
                }
                List<String> row = new ArrayList<>();
                if (i == 0) {
                    row.add(dpdlistid); // 1
                    row.add(modifiedCustomerNo); // 2
                    row.add(cusShortName); // 3
                } else {
                    row.add("");
                    row.add("");
                    row.add("");
                }

                row.add(date); // DATE 4
                row.add(currDpd); // CUR.DPD 5
                row.add(curDpdRef); // CUR.DPD.REF 6
                row.add(dpdStatus); // DPD.STATUS 7
                row.add(peakDpd); // PEAK.DPD 8
                row.add(peadDpdRef); // PEAK.DPD.REF 9
                row.add(dateFlagset); // DATE.FLAG.SET 10
                row.add(dateFlagRel);// DATE.FLAG.REL 11
                row.add(custLevelFlag);// CUST.LEVEL.FLAG 12
                returnValues.add(String.join("*", row));
                i++;
                totalRowsMatched++;
            }

        }
        return returnValues;
    }

}