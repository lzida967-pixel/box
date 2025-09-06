package com.chatapp.dto;

import com.chatapp.entity.Friendship;
import com.chatapp.entity.User;

/**
 * 好友关系与用户信息DTO
 * 用于在API响应中包含好友关系和相关用户信息
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public class FriendshipWithUserDTO {
    private Friendship friendship;
    private User fromUser; // 发送请求的用户（收到的请求中使用）
    private User toUser; // 接收请求的用户（发送的请求中使用）

    public FriendshipWithUserDTO() {
    }

    public FriendshipWithUserDTO(Friendship friendship, User fromUser, User toUser) {
        this.friendship = friendship;
        this.fromUser = fromUser;
        this.toUser = toUser;
    }

    // Getter and Setter methods
    public Friendship getFriendship() {
        return friendship;
    }

    public void setFriendship(Friendship friendship) {
        this.friendship = friendship;
    }

    public User getFromUser() {
        return fromUser;
    }

    public void setFromUser(User fromUser) {
        this.fromUser = fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public void setToUser(User toUser) {
        this.toUser = toUser;
    }

    @Override
    public String toString() {
        return "FriendshipWithUserDTO{" +
                "friendship=" + friendship +
                ", fromUser=" + fromUser +
                ", toUser=" + toUser +
                '}';
    }
}