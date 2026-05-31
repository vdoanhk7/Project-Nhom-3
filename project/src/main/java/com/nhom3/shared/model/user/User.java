package com.nhom3.shared.model.user;

import com.nhom3.shared.model.Entity;

public abstract class User extends Entity{
    public static final int DEFAULT_REPUTATION_SCORE = 100;

    protected UserInfo userInfo;
    protected UserContact userContact;
    protected final Role role;
    protected int reputationScore;
    protected double userRatingAverage;
    protected int userRatingCount;

    public User(int id, UserInfo userInfo, UserContact userContact, Role role) {
        super(id);
        this.userInfo = userInfo;
        this.userContact = userContact;
        this.role = role;
        this.reputationScore = DEFAULT_REPUTATION_SCORE;
        this.userRatingAverage = 0;
        this.userRatingCount = 0;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public UserContact getUserContact() {
        return userContact;
    }

    public Role getRole() {return role;}

    public int getReputationScore() {
        return reputationScore;
    }

    public void setReputationScore(int reputationScore) {
        this.reputationScore = Math.max(0, Math.min(DEFAULT_REPUTATION_SCORE, reputationScore));
    }

    public double getUserRatingAverage() {
        return userRatingAverage;
    }

    public int getUserRatingCount() {
        return userRatingCount;
    }

    public void setUserRatingSummary(double userRatingAverage, int userRatingCount) {
        this.userRatingAverage = Math.max(0, Math.min(5, userRatingAverage));
        this.userRatingCount = Math.max(0, userRatingCount);
    }

    protected String formatUserInfo(String roleName) {
        String username = userInfo == null ? "N/A" : userInfo.getUserName();
        String fullName = userInfo == null ? "N/A" : userInfo.getName();
        String email = userContact == null ? "N/A" : userContact.getEmail();
        String phone = userContact == null ? "N/A" : userContact.getPhoneNumber();

        return String.format(
                "%s[id=%d, username=%s, fullName=%s, email=%s, phone=%s, reputation=%d, userRating=%.2f/%d]",
                roleName,
                id,
                username,
                fullName,
                email,
                phone,
                reputationScore,
                userRatingAverage,
                userRatingCount);
    }
    
}
