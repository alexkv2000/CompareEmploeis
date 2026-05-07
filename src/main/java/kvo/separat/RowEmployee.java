package kvo.separat;

import java.util.Objects;

@SuppressWarnings("java:S107")
public class RowEmployee implements SyncEntity {

    // Исправлено: поменяли модификатор доступа на private
    private String employeeID;
    private String lastnameRUS;
    private String nameRUS;
    private String middleNameRUS;
    private String tabNom;
    private String jobTitleRUS;
    private String loginName;
    private String email;
    private String ipPhone;
    private String workPhone;
    private String typeWork;
    private String departmentID;
    private String managerID;
    private String userSID;
    private String dateCreate;

    public RowEmployee(String employeeID, String lastnameRUS, String nameRUS, String middleNameRUS, String tabNom, String jobTitleRUS, String loginName, String email, String ipPhone, String workPhone, String typeWork, String departmentID, String managerID, String userSID, String dateCreate) {
        this.employeeID = employeeID;
        this.lastnameRUS = lastnameRUS;
        this.nameRUS = nameRUS;
        this.middleNameRUS = middleNameRUS;
        this.tabNom = tabNom;
        this.jobTitleRUS = jobTitleRUS;
        this.loginName = loginName;
        this.email = email;
        this.ipPhone = ipPhone;
        this.workPhone = workPhone;
        this.typeWork = typeWork;
        this.departmentID = departmentID;
        this.managerID = managerID;
        this.userSID = userSID;
        this.dateCreate = dateCreate;
    }

    // --- ГЕТТЕРЫ ---
    public String getEmployeeID() { return employeeID; }
    public String getLastnameRUS() { return lastnameRUS; }
    public String getNameRUS() { return nameRUS; }
    public String getMiddleNameRUS() { return middleNameRUS; }
    public String getTabNom() { return tabNom; }
    public String getJobTitleRUS() { return jobTitleRUS; }
    public String getLoginName() { return loginName; }
    public String getEmail() { return email; }
    public String getIpPhone() { return ipPhone; }
    public String getWorkPhone() { return workPhone; }
    public String getTypeWork() { return typeWork; }
    public String getDepartmentID() { return departmentID; }
    public String getManagerID() { return managerID; }
    public String getUserSID() { return userSID; }
    public String getDateCreate() { return dateCreate; }

    @Override
    public String getId() {
        return getEmployeeID(); // Используем геттер
    }

    @Override
    public String getDataForHashing() {
        return employeeID + "|" + lastnameRUS + "|" + nameRUS + "|" + middleNameRUS + "|" +
                tabNom + "|" + jobTitleRUS + "|" + loginName + "|" + email + "|" + ipPhone + "|" +
                workPhone + "|" + typeWork + "|" + departmentID + "|" + managerID + "|" + userSID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RowEmployee that = (RowEmployee) o;

        return Objects.equals(employeeID, that.employeeID) &&
                Objects.equals(lastnameRUS, that.lastnameRUS) &&
                Objects.equals(nameRUS, that.nameRUS) &&
                Objects.equals(middleNameRUS, that.middleNameRUS) &&
                Objects.equals(tabNom, that.tabNom) &&
                Objects.equals(jobTitleRUS, that.jobTitleRUS) &&
                Objects.equals(loginName, that.loginName) &&
                Objects.equals(email, that.email) &&
                Objects.equals(ipPhone, that.ipPhone) &&
                Objects.equals(workPhone, that.workPhone) &&
                Objects.equals(typeWork, that.typeWork) &&
                Objects.equals(departmentID, that.departmentID) &&
                Objects.equals(managerID, that.managerID) &&
                Objects.equals(userSID, that.userSID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeID, lastnameRUS, nameRUS, middleNameRUS, tabNom, jobTitleRUS,
                loginName, email, ipPhone, workPhone, typeWork, departmentID, managerID, userSID);
    }
}