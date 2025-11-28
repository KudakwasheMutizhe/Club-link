package com.clublink.club_link;

public class ClubModal {

    private String clubName;
    private String shortDescription;
    private String imagePath;
    private String clubId;
    private String category;   // ⬅️ NEW FIELD

    // Required empty constructor for Firebase
    public ClubModal() {}

    // Optional convenience constructor
    public ClubModal(String imagePath, String clubName, String shortDescription, String category) {
        this.imagePath = imagePath;
        this.clubName = clubName;
        this.shortDescription = shortDescription;
        this.category = category;
    }

    // Getters and setters

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getClubId() {
        return clubId;
    }

    public void setClubId(String clubId) {
        this.clubId = clubId;
    }

    public String getCategory() {          // ⬅️ NEW GETTER
        return category;
    }

    public void setCategory(String category) {  // ⬅️ NEW SETTER
        this.category = category;
    }
}