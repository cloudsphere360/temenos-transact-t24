package com.temenos.fusion;

import java.util.ArrayList;
import java.util.List;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebffloandpd.DateClass;
import com.temenos.t24.api.records.ebffloandpd.EbFfLoanDpdRecord;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

public class FfNofileloanDpdRtn extends Enquiry {

    // ===== Logging =====
    
    //private static final Logger LOGG = LoggerFactory.getLogger(L3API);
    // Flip to false to reduce noise after debugging
    private static final boolean DEBUG = true;

    private final DataAccess da = new DataAccess(this);
    private final Session session = new Session();

    // Working fields (kept like your structure)
    String loanId = "";
    String customerName = "";
    String date = "";
    String currDpd = "";
    String curDpdRef = "";
    String dpdStatus = "";
    String peakDpd = "";
    String peadDpdRef = "";
    String peakCollDate = "";

    // ============================
    // FILTER STATE
    // ============================
    String fLoanId = "";         String opLoanId = "";
    String fCustNo = "";         String opCustNo = "";
    String fCustName = "";       String opCustName = "";

    String fDateEq = "";
    String fDateFrom = "";
    String fDateTo = "";
    String opDate = "";

    String fCurDpd = "";         String opCurDpd = "";
    String fDpdStatus = "";      String opDpdStatus = "";
    String fPeakDpd = "";        String opPeakDpd = "";
    String fPeakDpdRef = "";     String opPeakDpdRef = "";

    String fPeakCollDateEq = "";
    String fPeakCollFrom = "";
    String fPeakCollTo = "";
    String opPeakCollDate = "";

    // ============================
    // Helper: safe & normalization
    // ============================
    private static String safe(String s) { return s == null ? "" : s.trim(); }
    private static boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }

    private static boolean isYYYYMMDD(String s) { return s != null && s.matches("\\d{8}"); }
    private static int ymdCompare(String a, String b) { return a.compareTo(b); }

    // ============================
    // Helper: operand checks
    // (Supports numeric codes & text. Adjust numeric codes if your site differs.)
    // ============================
    private static boolean isEQ(String op) { op = safe(op).toUpperCase(); return op.equals("1") || op.equals("EQ"); }
    private static boolean isRG(String op) { op = safe(op).toUpperCase(); return op.equals("2") || op.equals("RG"); }
    private static boolean isLT(String op) { op = safe(op).toUpperCase(); return op.equals("3") || op.equals("LT"); }
    private static boolean isGT(String op) { op = safe(op).toUpperCase(); return op.equals("4") || op.equals("GT"); }

    // If your site uses different numeric codes for LE / GE, adjust here
    private static boolean isLE(String op) { op = safe(op).toUpperCase(); return op.equals("8") || op.equals("LE"); }
    
    private static boolean isGE(String op) { op = safe(op).toUpperCase(); return op.equals("9") || op.equals("GE"); }

    // Contains (CT) code differs per site; include common cases + "CT"
    private static boolean isCT(String op) { op = safe(op).toUpperCase(); return op.equals("6")|| op.equals("CT"); }

    // ============================
    // Comparators
    // ============================
    private boolean matchNumber(String actual, String filterVal, String op) {
        if (isEmpty(filterVal)) return true;
        if (isEmpty(actual))    return false;

        double a, b;
        try {
            a = Double.parseDouble(actual.trim());
            b = Double.parseDouble(filterVal.trim());
        } catch (Exception e) {
            return false;
        }

        if (isEQ(op)) return Double.compare(a, b) == 0;
        if (isLT(op)) return a <  b;
        if (isGT(op)) return a >  b;
        if (isLE(op)) return a <= b;
        if (isGE(op)) return a >= b;
        return true; // unknown op -> allow
    }

    private boolean matchText(String actual, String filterVal, String op) {
        if (isEmpty(filterVal)) return true;
        actual = safe(actual);
        //String f = safe(filterVal);
        String f = safe(filterVal).trim();
        f = f.replaceAll("^\\.+|\\.+$", "");
        if (isEQ(op)) return actual.equals(f);
        if (isCT(op)) return actual.toUpperCase().contains(f.toUpperCase());
        return true; // unknown op -> allow
    }

    /**
     * DATE / PEAK.COLL.DATE matching (strict when a filter is present):
     *  - If no filter provided at all (op, eqVal, from, to all empty) => return true (do not filter).
     *  - EQ => requires eqVal "YYYYMMDD" and actual must be valid & equal.
     *  - RG => requires from/to "YYYYMMDD" and actual must be valid & within [from,to].
     */
    private boolean matchDateField(String actual, String eqVal, String from, String to, String op) {
        // If no filter provided for this field, do not filter the row.
        if (isEmpty(op) && isEmpty(eqVal) && isEmpty(from) && isEmpty(to)) {
            return true;
        }

        actual = safe(actual);
        if (!isYYYYMMDD(actual)) return false; // we only validate actual if a filter exists

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
            if (!isYYYYMMDD(eqVal)) return false;
            return actual.equals(eqVal);
        }

        if (isRG(opNorm)) {
            from = safe(from);
            to   = safe(to);
            if (!isYYYYMMDD(from) || !isYYYYMMDD(to)) return false;
            return ymdCompare(actual, from) >= 0 && ymdCompare(actual, to) <= 0;
        }

        // Unspecified/unknown op with some values -> be permissive
        return true;
    }

    // ============================
    // Fetch CUST.NO & CUST.NAME from AA.ARRANGEMENT (first part of LOAN.ID before '-')
    // ============================
    private String[] getCustomerInfoForLoan(String finmnemonic, String loanIdFull) {
        String[] result = new String[] { "", "" }; // [CUST.NO, CUST.NAME]
        String id = safe(loanIdFull);
        if (id.isEmpty()) return result;

        String[] parts = id.split("-");
        String arrangementId = parts.length > 0 ? safe(parts[0]) : id;

        try {
            AaArrangementRecord aa = new AaArrangementRecord(
                da.getRecord(finmnemonic, "AA.ARRANGEMENT", "", arrangementId)
            );

            String customerId = "";
            try { customerId = safe(aa.getCustomer().get(0).getCustomer().getValue()); } catch (Exception ignore) {}

            if (!customerId.isEmpty()) {
                CustomerRecord cust = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
                String shortName = "";
                try { shortName = safe(cust.getShortName().get(0).getValue()); }
                catch (Exception e) {
                   
                }
                result[0] = customerId;   // CUST.NO (not printed in output because your header has 9 cols)
                result[1] = shortName;    // CUST.NAME
            }
        } catch (Exception e) {
              }
        return result;
    }

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        List<String> returnValues = new ArrayList<>();

        // Read filters
        for (FilterCriteria filter : filterCriteria) {
            String field = safe(filter.getFieldname()).toUpperCase();
            String value = safe(filter.getValue());
            String op    = safe(filter.getOperand());

            switch (field) {
                case "LOAN.ID":
                    fLoanId = value; opLoanId = op; break;

                case "CUST.NO":
                    fCustNo = value; opCustNo = op; break;

                case "CUST.NAME":
                    fCustName = value; opCustName = op; break;

                case "DATE":
                    opDate = op;
                    if (isEQ(op)) {
                        fDateEq = value;
                    } else if (isRG(op)) {
                        String[] d = value.split("\\s+");
                        if (d.length > 0) fDateFrom = d[0];
                        if (d.length > 1) fDateTo   = d[1];
                    } else {
                        // Operator missing? auto-detect by tokens
                        String[] d = value.split("\\s+");
                        if (d.length == 1) { opDate = "EQ"; fDateEq = d[0]; }
                        else if (d.length >= 2) { opDate = "RG"; fDateFrom = d[0]; fDateTo = d[1]; }
                    }
                    break;

                case "CUR.DPD":
                    fCurDpd = value;
                    opCurDpd = op;
                    break;
                    

                case "DPD.STATUS":
                    fDpdStatus = value; opDpdStatus = op; break;

                case "PEAK.DPD":
                    fPeakDpd = value;
                    opPeakDpd = op;
                    break;
                    

                case "PEAK.DPD.REF":
                    fPeakDpdRef = value; opPeakDpdRef = op; break;

                case "PEAK.COLL.DATE":
                    opPeakCollDate = op;
                    if (isEQ(op)) {
                        fPeakCollDateEq = value;
                    } else if (isRG(op)) {
                        String[] d2 = value.split("\\s+");
                        if (d2.length > 0) fPeakCollFrom = d2[0];
                        if (d2.length > 1) fPeakCollTo   = d2[1];
                    } else {
                        // Auto-detect if missing
                        String[] d2 = value.split("\\s+");
                        if (d2.length == 1) { opPeakCollDate = "EQ"; fPeakCollDateEq = d2[0]; }
                        else if (d2.length >= 2) { opPeakCollDate = "RG"; fPeakCollFrom = d2[0]; fPeakCollTo = d2[1]; }
                    }
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
            dpdlist = da.selectRecords(finmnemonic, "EB.FF.LOAN.DPD", "", "");
        } catch (Exception e) {
            dpdlist = new ArrayList<>();
        }

        //LOGG.info("Company=" + finmnemonic + ", EB.FF.LOAN.DPD records=" + dpdlist.size());

        if (dpdlist.isEmpty()) {
            //LOGG.info("[END DPD ENQUIRY DEBUG] -> No records in EB.FF.LOAN.DPD");
            return returnValues;
        }

        // Global counters for reasons
        int totalLoansVisited = 0;
        int totalRowsVisited = 0;
        int totalRowsMatched = 0;
        int failDate = 0, failCurDpd = 0, failDpdStatus = 0, failPeakDpd = 0, failPeakRef = 0, failPeakColl = 0;
        int skipLoanByLoanId = 0, skipLoanByCustNo = 0, skipLoanByCustName = 0;

        for (String dpdlistid : dpdlist) {
            totalLoansVisited++;

            // Record-level: LOAN.ID
            if (!matchText(dpdlistid, fLoanId, opLoanId)) {
                skipLoanByLoanId++;
                if (DEBUG) //LOGG.info("Skip loan by LOAN.ID -> " + dpdlistid);
                continue;
            }

            // Fetch customer (needed for CUST filters and first row output)
            String[] cust = getCustomerInfoForLoan(finmnemonic, dpdlistid);
            String customerNum = cust[0];
            String customerShortName = cust[1];

            // Record-level: CUST.NO, CUST.NAME
            if (!matchText(customerNum, fCustNo, opCustNo)) {
                skipLoanByCustNo++;
                if (DEBUG) //LOGG.info("Skip loan by CUST.NO -> " + dpdlistid + " custNo=" + customerNum);
                continue;
            }
            if (!matchText(customerShortName, fCustName, opCustName)) {
                skipLoanByCustName++;
                if (DEBUG) //LOGG.info("Skip loan by CUST.NAME -> " + dpdlistid + " custName=" + customerShortName);
                continue;
            }

            // Load DPD rows
            EbFfLoanDpdRecord dpdRecord =
                new EbFfLoanDpdRecord(da.getRecord(finmnemonic, "EB.FF.LOAN.DPD", "", dpdlistid));

            List<DateClass> dateList = dpdRecord.getDate();
            if (dateList == null || dateList.isEmpty()) {
                if (DEBUG) //LOGG.info("Loan has no DATE MV -> " + dpdlistid);
                continue;
            }

            int i = 0;
            int matchedInThisLoan = 0;

            if (DEBUG) {
                // Log a quick min/max/first few dates to see what’s present
                String minD = null, maxD = null;
                int count = 0;
                for (DateClass d : dateList) {
                    String dd = safe(d.getDate().getValue());
                    if (!isYYYYMMDD(dd)) continue;
                    if (minD == null || dd.compareTo(minD) < 0) minD = dd;
                    if (maxD == null || dd.compareTo(maxD) > 0) maxD = dd;
                    if (count < 2) //LOGG.info("Sample date row loan=" + dpdlistid + " date=" + dd);
                    count++;
                }
                //LOGG.info("Loan=" + dpdlistid + " dates[min=" + minD + ", max=" + maxD + ", total=" + dateList.size() + "]");
            }

            for (DateClass dates : dateList) {
                totalRowsVisited++;

                date         = safe(dates.getDate().getValue());
                currDpd      = safe(dates.getCurDpd().getValue());
                curDpdRef    = safe(dates.getCurDpdRef().getValue());
                dpdStatus    = safe(dates.getDpdStatus().getValue());
                peakDpd      = safe(dates.getPeakDpd().getValue());
                peadDpdRef   = safe(dates.getPeakDpdRef().getValue());
                peakCollDate = safe(dates.getPeakDpdCollDate().getValue());

                // Row-level filters with reason tracking
                boolean dateOk      = matchDateField(date, fDateEq, fDateFrom, fDateTo, opDate);
                boolean curOk       = matchNumber(currDpd, fCurDpd, opCurDpd);
                boolean dpdStatOk   = matchText(dpdStatus, fDpdStatus, opDpdStatus);
                boolean peakOk      = matchNumber(peakDpd, fPeakDpd, opPeakDpd);
                boolean peakRefOk   = matchText(peadDpdRef, fPeakDpdRef, opPeakDpdRef);
                boolean peakCollOk  = matchDateField(peakCollDate, fPeakCollDateEq, fPeakCollFrom, fPeakCollTo, opPeakCollDate);

                if (!(dateOk && curOk && dpdStatOk && peakOk && peakRefOk && peakCollOk)) {
                    if (DEBUG) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Row FAIL loan=").append(dpdlistid)
                          .append(" date=").append(date)
                          .append(" [");
                        if (!dateOk)     { sb.append("DATE "); failDate++; }
                        if (!curOk)      { sb.append("CUR.DPD "); failCurDpd++; }
                        if (!dpdStatOk)  { sb.append("DPD.STATUS "); failDpdStatus++; }
                        if (!peakOk)     { sb.append("PEAK.DPD "); failPeakDpd++; }
                        if (!peakRefOk)  { sb.append("PEAK.DPD.REF "); failPeakRef++; }
                        if (!peakCollOk) { sb.append("PEAK.COLL.DATE "); failPeakColl++; }
                        sb.append("]");
                    }
                    continue;
                }

                // ============================
                // BUILD OUTPUT ROW (9 columns)
                // HEADER:
                // LOAN.ID / CUST.NAME / DATE / CUR.DPD / CUR.DPD.REF / DPD.STATUS / PEAK.DPD / PEAK.DPD.REF / PEAK.COLL.DATE
                // ============================
                List<String> row = new ArrayList<>();
                if (i == 0) {
                    row.add(dpdlistid);           // LOAN.ID
                    row.add(customerShortName);   // CUST.NAME
                } else {
                    row.add(""); 
                    row.add("");
                }
                row.add(date);          // DATE
                row.add(currDpd);       // CUR.DPD
                row.add(curDpdRef);     // CUR.DPD.REF
                row.add(dpdStatus);     // DPD.STATUS
                row.add(peakDpd);       // PEAK.DPD
                row.add(peadDpdRef);    // PEAK.DPD.REF
                row.add(peakCollDate);  // PEAK.COLL.DATE

                returnValues.add(String.join("*", row));
                i++;
                matchedInThisLoan++;
                totalRowsMatched++;
            }

            
        }

        return returnValues;
    }
}