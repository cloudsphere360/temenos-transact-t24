package com.temenos.fusion;

/*-----------------------------------------------------------------------------
 * @author Vinothini P
 * Date Created:
 * Attached as : Default Routine
 * EB.API : NA
 * Attached to :NA
 * Description: this routine is used default the given name.
 *------------------------------------------------------------------------------ 
 * Modification History :
 *----------------------------------------------------------------------------- 
 *22-Aug-2025   Development      Initial Version
 *-----------------------------------------------------------------------------
 *18-FEB-2026   Defect-TEM-293      by subash
 */

import com.temenos.api.TStructure;
import com.temenos.logging.facade.Logger;
import com.temenos.logging.facade.LoggerFactory;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.system.DataAccess;

public class FfDefCustGivenNameUpd extends RecordLifecycle {
    private static final String L3API = "L3API";
    private static final Logger LOGGER = LoggerFactory.getLogger(L3API);

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        try {
            LOGGER.info("defaultFieldValues:DefCustGivenName routine triggered");
            DataAccess da = new DataAccess(this);
            CustomerRecord customerObj = new CustomerRecord(currentRecord);
            LOGGER.info(" Customer record before update " + customerObj.toString());
            String cmpId = customerObj.getCompanyBook().getValue();

            CompanyRecord companyRec = new CompanyRecord(da.getRecord("COMPANY", cmpId));
            customerObj.getLocalRefField("FF.BRANCH.NAME").setValue(companyRec.getCompanyName().get(0).getValue());
            LOGGER.info(" after update cusRec " + customerObj.toString());

            defaultNames(customerObj); // changes done as part of Defect-TEM on 18thfeb

            String mnemonic = "C" + currentRecordId;
            customerObj.setMnemonic(mnemonic);

            int one = 1;
            if (customerObj.getNoOfDependents().getValue().isEmpty()) {

                customerObj.setNoOfDependents("1");
            } else {
                String noOfDependants = customerObj.getNoOfDependents().getValue();
                int noOfDep = Integer.parseInt(noOfDependants);
                int addDep = noOfDep + one;
                String convertnoOfDep = String.valueOf(addDep);
                customerObj.setNoOfDependents(convertnoOfDep);
            }
            LOGGER.info(" Customer record after update " + customerObj.toString());
            currentRecord.set(customerObj.toStructure());
        } catch (Exception e) {
            LOGGER.error("Error in defaultFieldValues:DefCustGivenName", e);

        }
    }

    /**
     * @param customerObj
     */
    private void defaultNames(CustomerRecord customerObj) {
        String firstName = getName(customerObj, "FIRST");
        String middleName = getName(customerObj, "MIDDLE");
        String lastName = getName(customerObj, "LAST");
        setName1(customerObj, firstName);

        if (!firstName.equals("")) {
            String givenName = getGivenName(firstName, middleName, lastName);
            if (givenName.length() > 50) {
                givenName = givenName.substring(0, 50);
            }
            customerObj.setGivenNames(givenName);
        }

    }

    /**
     * @param firstName
     * @param middleName
     * @param lastName
     * @return
     */
    private String getGivenName(String firstName, String middleName, String lastName) {

        StringBuilder sb = new StringBuilder();
        sb.append(firstName);
        if (!middleName.equals("")) {
            sb.append(" ");
            sb.append(middleName);
        }
        if (!lastName.equals("")) {
            sb.append(" ");
            sb.append(lastName);
        }

        return sb.toString();
    }

    /**
     * @param customerObj
     * @param firstname
     */
    private void setName1(CustomerRecord customerObj, String firstname) {
        try {
            customerObj.getName1(0).setValue(firstname);
        } catch (Exception e) {
            customerObj.addName1(firstname);
        }

    }

    /**
     * @param customerObj
     * @param string
     * @return
     */
    private String getName(CustomerRecord customerObj, String value) {
        try {
            if (value.equals("FIRST")) {
                return customerObj.getShortName().get(0).getValue();
            } else if (value.equals("MIDDLE")) {
                return customerObj.getName2(0).getValue();
            } else if (value.equals("LAST")) {
                return customerObj.getFamilyName().getValue();
            }
        } catch (Exception e) {
        }
        return "";
    }

}