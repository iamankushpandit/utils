package com.utilityexplorer.shared.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "source")
public class Source {

    @Id
    @Column(name = "source_id")
    private String sourceId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(name = "terms_url")
    private String termsUrl;

    @Column(name = "attribution_text")
    private String attributionText;

    private String notes;

    @Column(name = "is_mock", nullable = false)
    private boolean isMock = false;

    // Constructors
    public Source() {}

    public Source(String sourceId, String name, String type, String termsUrl,
                  String attributionText, String notes) {
        this.sourceId = sourceId;
        this.name = name;
        this.type = type;
        this.termsUrl = termsUrl;
        this.attributionText = attributionText;
        this.notes = notes;
    }

    // Getters and setters
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getTermsUrl() { return termsUrl; }
    public void setTermsUrl(String termsUrl) { this.termsUrl = termsUrl; }

    public String getAttributionText() { return attributionText; }
    public void setAttributionText(String attributionText) { this.attributionText = attributionText; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isMock() { return isMock; }
    public void setMock(boolean mock) { isMock = mock; }
}
