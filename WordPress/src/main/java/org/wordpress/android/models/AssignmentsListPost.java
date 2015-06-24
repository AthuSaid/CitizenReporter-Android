package org.wordpress.android.models;

import android.text.format.DateUtils;

import org.wordpress.android.util.StringUtils;

import java.util.Date;

/**
 * Barebones post/page as listed in AssigmnentsListFragment
 */
public class AssignmentsListPost {
    private int postId;
    private int blogId;
    private String title;
    private String excerpt;
    private long dateCreatedGmt;
    private String status;
    private boolean isLocalDraft;
    private boolean hasLocalChanges;
    private boolean mIsUploading;

    public AssignmentsListPost(int postId, int blogId, String title, String excerpt, long dateCreatedGmt, String status, boolean localDraft,
                               boolean localChanges, boolean uploading) {
        setPostId(postId);
        setBlogId(blogId);
        setTitle(title);
        setExcerpt(excerpt);
        setDateCreatedGmt(dateCreatedGmt);
        setStatus(status);
        setLocalDraft(localDraft);
        setHasLocalChanges(localChanges);
        setIsUploading(uploading);
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public int getBlogId() {
        return blogId;
    }

    public void setBlogId(int blogId) {
        this.blogId = blogId;
    }

    public String getTitle() {
        return StringUtils.notNullStr(title);
    }
    public String getExcerpt() {
        return StringUtils.notNullStr(excerpt);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getDateCreatedGmt() {
        return dateCreatedGmt;
    }

    public void setDateCreatedGmt(long dateCreatedGmt) {
        this.dateCreatedGmt = dateCreatedGmt;
    }

    public String getOriginalStatus() {
        return StringUtils.notNullStr(status);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFormattedDate() {
        return DateUtils.getRelativeTimeSpanString(getDateCreatedGmt(), new Date().getTime(), DateUtils.SECOND_IN_MILLIS).toString();
    }

    public boolean isLocalDraft() {
        return isLocalDraft;
    }

    public void setLocalDraft(boolean isLocalDraft) {
        this.isLocalDraft = isLocalDraft;
    }

    public boolean hasLocalChanges() {
        return hasLocalChanges;
    }

    public void setHasLocalChanges(boolean localChanges) {
        this.hasLocalChanges = localChanges;
    }

    public boolean isUploading() {
        return mIsUploading;
    }

    public void setIsUploading(boolean uploading) {
        this.mIsUploading = uploading;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }
}
