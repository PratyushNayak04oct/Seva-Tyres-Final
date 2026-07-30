package com.sevatyres.model;

/**
 * Singleton company profile used on invoices, emails, and SMS identity.
 */
public class CompanyInfo {

    private int id = 1;
    private String companyName = "Seva Tyres";
    private String ownerName;
    private String email;
    private String phone;
    private String dbtPhone;      // DBT / SMS-enabled number
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String gstin;
    private String bankName;
    private String bankAccount;
    private String bankIfsc;
    private String upiId;
    private String aboutText;
    private String supportEmail;
    private String supportPhone;

    public CompanyInfo() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDbtPhone() { return dbtPhone; }
    public void setDbtPhone(String dbtPhone) { this.dbtPhone = dbtPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

    public String getBankIfsc() { return bankIfsc; }
    public void setBankIfsc(String bankIfsc) { this.bankIfsc = bankIfsc; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getAboutText() { return aboutText; }
    public void setAboutText(String aboutText) { this.aboutText = aboutText; }

    public String getSupportEmail() { return supportEmail; }
    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }

    public String getSupportPhone() { return supportPhone; }
    public void setSupportPhone(String supportPhone) { this.supportPhone = supportPhone; }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (address != null && !address.isBlank()) sb.append(address.trim());
        String cityLine = "";
        if (city != null && !city.isBlank()) cityLine += city.trim();
        if (state != null && !state.isBlank()) {
            if (!cityLine.isEmpty()) cityLine += ", ";
            cityLine += state.trim();
        }
        if (pincode != null && !pincode.isBlank()) {
            if (!cityLine.isEmpty()) cityLine += " — ";
            cityLine += pincode.trim();
        }
        if (!cityLine.isEmpty()) {
            if (!sb.isEmpty()) sb.append('\n');
            sb.append(cityLine);
        }
        return sb.toString();
    }

    public String getContactLine() {
        String em = supportEmail != null && !supportEmail.isBlank() ? supportEmail
                : (email != null ? email : "");
        String ph = supportPhone != null && !supportPhone.isBlank() ? supportPhone
                : (phone != null ? phone : "");
        if (!em.isBlank() && !ph.isBlank()) return em + " · " + ph;
        if (!em.isBlank()) return em;
        return ph;
    }
}
