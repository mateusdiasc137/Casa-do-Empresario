package com.casaempresario.app.repository;

import android.content.Context;
import com.casaempresario.app.repository.room.RoomUserRepository;
import com.casaempresario.app.repository.room.RoomEventRepository;
import com.casaempresario.app.repository.room.RoomInterestRepository;
import com.casaempresario.app.repository.room.RoomChatRepository;
import com.casaempresario.app.repository.room.RoomPhotoRepository;
import com.casaempresario.app.repository.firebase.FirebaseUserRepository;
import com.casaempresario.app.repository.firebase.FirebaseEventRepository;
import com.casaempresario.app.repository.firebase.FirebaseInterestRepository;
import com.casaempresario.app.repository.firebase.FirebaseChatRepository;
import com.casaempresario.app.repository.firebase.FirebasePhotoRepository;

public class RepositoryProvider {
    // Altere para true para mudar o aplicativo INTEIRO para usar o Firebase!
    private static final boolean USE_FIREBASE = true;

    public static UserRepository getUserRepository(Context context) {
        if (USE_FIREBASE) {
            return new FirebaseUserRepository();
        } else {
            return new RoomUserRepository(context);
        }
    }

    public static EventRepository getEventRepository(Context context) {
        if (USE_FIREBASE) {
            return new FirebaseEventRepository();
        } else {
            return new RoomEventRepository(context);
        }
    }

    public static InterestRepository getInterestRepository(Context context) {
        if (USE_FIREBASE) {
            return new FirebaseInterestRepository();
        } else {
            return new RoomInterestRepository(context);
        }
    }

    public static ChatRepository getChatRepository(Context context) {
        if (USE_FIREBASE) {
            return new FirebaseChatRepository();
        } else {
            return new RoomChatRepository(context);
        }
    }

    public static PhotoRepository getPhotoRepository(Context context) {
        if (USE_FIREBASE) {
            return new FirebasePhotoRepository();
        } else {
            return new RoomPhotoRepository(context);
        }
    }
}
