# Firestore Security Rules для чата с психологом

В проекте пока нет отдельного файла правил Firestore, поэтому ниже — готовый блок, который можно вставить в ваш основной `firestore.rules`.

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isSignedIn() {
      return request.auth != null;
    }

    function isPsychChatParticipant() {
      return isSignedIn()
        && request.auth.uid in resource.data.participantIds;
    }

    function isPsychChatParticipantOnCreate() {
      return isSignedIn()
        && request.auth.uid in request.resource.data.participantIds
        && request.resource.data.participantIds.size() == 2;
    }

    match /psychChats/{chatId} {
      allow read: if isPsychChatParticipant();
      allow create: if isPsychChatParticipantOnCreate();
      allow update: if isPsychChatParticipant();
      allow delete: if false;

      match /messages/{messageId} {
        allow read: if isPsychChatParticipant();
        allow create: if isPsychChatParticipant()
          && request.resource.data.senderId == request.auth.uid
          && request.resource.data.chatId == chatId
          && request.resource.data.text is string
          && request.resource.data.text.size() > 0
          && request.resource.data.text.size() <= 4000;
        allow update, delete: if false;
      }
    }
  }
}
```

## Что это даёт
- читать чат и сообщения могут только участники диалога;
- создавать сообщения может только авторизованный участник;
- чужие психологи, студенты, преподаватели и директор не видят диалог;
- удаление и редактирование сообщений в MVP отключено.

## Куда вставить
Если вы используете Firebase Console:
1. Откройте **Firestore Database**
2. Перейдите в **Rules**
3. Вставьте блок в основной файл правил
4. Проверьте, чтобы не было конфликтов с уже существующими `match`-секциями
5. Опубликуйте правила

