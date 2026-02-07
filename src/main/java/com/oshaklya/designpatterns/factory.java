package com.oshaklya.designpatterns;

interface Notification {
    void sendNotification(String message);
}

class EmailNotification implements Notification {
    @Override
    public void sendNotification(String message) {
        System.out.println("sent email: " + message);
    }
}

class SMSNotification implements Notification {
    @Override
    public void sendNotification(String message) {
        System.out.println("sent sms: " + message);
    }
}

class NotificationFactory {
    public static Notification create(String type) throws Exception {
        if (type.equals("email")) {
            return new EmailNotification();
        } else if (type.equals("sms")) {
            return new SMSNotification();
        } else {
            throw new Exception();
        }
    }
}

class Factory {
    public static void main(String[] args) throws Exception {
        Notification notification1 = NotificationFactory.create("email");
        Notification notification2 = NotificationFactory.create("sms");
        notification1.sendNotification("hello");
        notification2.sendNotification("hello");
    }
}