package kvo.separat;

import java.util.Objects;

@SuppressWarnings("java:S107")
public class RowDepartments implements SyncEntity {
    private String departmentID;
    private String name;
    private String managerID;
    private String managerLoginName;
    private String parentID;
    private String typeName;
    private String code;
    private String bDate;
    private String eDate;
    private String dataIntegration;
    private String eDoc;
    private String idDeptOwn;
    private String dateCreate;

    public RowDepartments(String departmentID, String name, String managerID, String managerLoginName, String parentID, String typeName, String code, String bDate, String eDate, String dataIntegration, String eDoc, String idDeptOwn, String dateCreate) {
        this.departmentID = departmentID;
        this.name = name;
        this.managerID = managerID;
        this.managerLoginName = managerLoginName;
        this.parentID = parentID;
        this.typeName = typeName;
        this.code = code;
        this.bDate = bDate;
        this.eDate = eDate;
        this.dataIntegration = dataIntegration;
        this.eDoc = eDoc;
        this.idDeptOwn = idDeptOwn;
        this.dateCreate = dateCreate;
    }

    @Override public String getId() { return departmentID; }

    public void setName(String name) {
        this.name = name;
    }

    public void setManagerID(String managerID) {
        this.managerID = managerID;
    }

    public void setManagerLoginName(String managerLoginName) {
        this.managerLoginName = managerLoginName;
    }

    public void setParentID(String parentID) {
        this.parentID = parentID;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setbDate(String bDate) {
        this.bDate = bDate;
    }

    public void seteDate(String eDate) {
        this.eDate = eDate;
    }

    public void setDataIntegration(String dataIntegration) {
        this.dataIntegration = dataIntegration;
    }

    public void seteDoc(String eDoc) {
        this.eDoc = eDoc;
    }

    public void setIdDeptOwn(String idDeptOwn) {
        this.idDeptOwn = idDeptOwn;
    }

    public void setDateCreate(String dateCreate) {
        this.dateCreate = dateCreate;
    }

    public String getName() {
        return name;
    }

    public String getManagerID() {
        return managerID;
    }

    public String getManagerLoginName() {
        return managerLoginName;
    }

    public String getParentID() {
        return parentID;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getCode() {
        return code;
    }

    public String getbDate() {
        return bDate;
    }

    public String geteDate() {
        return eDate;
    }

    public String getDataIntegration() {
        return dataIntegration;
    }

    public String geteDoc() {
        return eDoc;
    }

    public String getIdDeptOwn() {
        return idDeptOwn;
    }

    public String getDateCreate() {
        return dateCreate;
    }

    @Override public String getDataForHashing() {
        return departmentID + "|" + name + "|" + managerID + "|" + managerLoginName + "|" + parentID + "|" + typeName + "|" + code + "|" + bDate + "|" + eDate + "|" + eDoc + "|" + idDeptOwn;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RowDepartments that = (RowDepartments) o;
        return Objects.equals(departmentID, that.departmentID) && Objects.equals(name, that.name) && Objects.equals(managerID, that.managerID) && Objects.equals(managerLoginName, that.managerLoginName) && Objects.equals(parentID, that.parentID) && Objects.equals(typeName, that.typeName) && Objects.equals(code, that.code) && Objects.equals(bDate, that.bDate) && Objects.equals(eDate, that.eDate) && Objects.equals(dataIntegration, that.dataIntegration) && Objects.equals(eDoc, that.eDoc) && Objects.equals(idDeptOwn, that.idDeptOwn);
    }

    @Override public int hashCode() {
        return Objects.hash(departmentID, name, managerID, managerLoginName, parentID, typeName, code, bDate, eDate, eDoc, idDeptOwn);
    }
}