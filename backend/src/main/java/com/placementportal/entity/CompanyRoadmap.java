package com.placementportal.entity;

import jakarta.persistence.*;

/** One step of a company's preparation roadmap, optionally linked to a Topic so the UI can jump to practice. */
@Entity
@Table(name = "company_roadmaps")
public class CompanyRoadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;                       // optional

    @Column(name = "resource_url", length = 500)
    private String resourceUrl;

    protected CompanyRoadmap() { }

    public CompanyRoadmap(Company company, int stepOrder, String title, String description,
                          Topic topic, String resourceUrl) {
        this.company = company;
        this.stepOrder = stepOrder;
        this.title = title;
        this.description = description;
        this.topic = topic;
        this.resourceUrl = resourceUrl;
    }

    public Long getId() { return id; }
    public Company getCompany() { return company; }
    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }
    public String getResourceUrl() { return resourceUrl; }
    public void setResourceUrl(String resourceUrl) { this.resourceUrl = resourceUrl; }
}
