package ivandjoh.online.springfirestore.service.impl;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import ivandjoh.online.springfirestore.http.request.FirebaseUserRequest;
import ivandjoh.online.springfirestore.http.request.HttpFirstQuestionRequest;
import ivandjoh.online.springfirestore.http.request.chatdetails.DataItem;
import ivandjoh.online.springfirestore.http.response.FirebaseResponse;
import ivandjoh.online.springfirestore.service.FirestoreService;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vo.ItemVO;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FirestoreServiceImpl implements FirestoreService {

    @Value("${firebase.collection-user}")
    private String collectionUser;

    @Value("${firebase.collection-chat}")
    private String collectionChat;

    @Override
    public ResponseEntity<?> getAllUsers()
            throws JsonParseException, JsonMappingException, IOException {
        String data = "{\"statusCode\": 200, \"status\": \"success\", \"statusMessage\": \"Hello World\"}";
        FirestoreServiceImpl json = new FirestoreServiceImpl();
        return ResponseEntity.ok(json.converter(data));
    }

    @Override
    public String saveUser(FirebaseUserRequest userRequest)
            throws JsonParseException, JsonMappingException, ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        String userId = userRequest.getUserId();
        String chatId = userRequest.getChatId();

        DocumentReference documentReference = db.collection(collectionUser).document(userId);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = future.get();


        if (document.exists()) {

            Map<String, Object> data = new HashMap<>();
            ArrayList<Object> ch = new ArrayList<>();
            Collections.addAll(ch, document.getData(), chatId);
            data.put(getSaltString(), chatId);
            ApiFuture<WriteResult> saveChat = db.collection(collectionUser).document(userId).set(data, SetOptions.merge());

            return saveChat.get().getUpdateTime().toString();
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put(getSaltString(), chatId);
            ApiFuture<WriteResult> future1 = db.collection(collectionUser).document(userId).set(data);

            return future1.get().getUpdateTime().toString();
        }

    }

    @Override
    public String saveChat(DataItem dataItem)
            throws JsonParseException, JsonMappingException, ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        Map<String, Object> docData = new HashMap<>();

        Map<String, Object> Pertanyaan = new HashMap<>();
        Pertanyaan.put("question_id", "62da699055be99482ab73257");
        Pertanyaan.put("content", "Anak saya demam, dok. Sudah 2 hari. Minum obat apa ya?");

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        docData.put("Pertanyaan - " + timestamp, Pertanyaan);
//        docData.put("Jawaban - 62da699055be99482ab73257", Pertanyaan);
        ApiFuture<WriteResult> future = db.collection("chats").document("62da699055be99482ab73257").set(docData, SetOptions.merge());
        return future.get().getUpdateTime().toString();
//        }
    }

    @Override
    public String saveFirstQuestion(HttpFirstQuestionRequest firstQuestion)
            throws JsonParseException, JsonMappingException, IOException, ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Map<String, Object> pertanyaan = new HashMap<>();
//        pertanyaan.put("doctor_id", firstQuestion.getDoctorId());
//        pertanyaan.put("user_id", firstQuestion.getUserId());
//        pertanyaan.put("content", firstQuestion.getContent());

        ArrayList<Object> tanya = new ArrayList<>();
        Collections.addAll(tanya, firstQuestion.getDoctorId(), firstQuestion.getUserId(), firstQuestion.getContent());
        pertanyaan.put("arrayExample", tanya);

        ApiFuture<WriteResult> future = db.collection(collectionChat).document(firstQuestion.getChatId()).set(pertanyaan);
        return future.get().getUpdateTime().toString();
    }

    @Override
    public void getAllItems(Consumer<List<ItemVO>> consumer) {
        Firestore firestore = FirestoreClient.getFirestore();
//        QuerySnapshot items = firestore.collectionGroup("items").get().get();
//        List<ItemVO> results = items.getDocuments().stream()
//                .map(d -> d.toObject(ItemVO.class))
//                .collect(Collectors.toList());
//        consumer.accept(results);

        // arrow method did not return errors when mapping wrong data objects (hence direct getting above get())
        Query items2 = firestore.collectionGroup("items");
        items2.addSnapshotListener((value, error) -> {
            if (value != null) {
                List<ItemVO> results2 = value.getDocuments()
                        .stream()
                        .map(d -> d.toObject(ItemVO.class))
                        .collect(Collectors.toList());
                consumer.accept(results2);
            }
            if(error != null){
                log.error("failed", error);
            }
        });
    }

    @Override
    public void insertItems() throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        List<ApiFuture<WriteResult>> futures = new ArrayList<>();
        CollectionReference items = firestore.collection("items");
        futures.add(items.document("item-1").set(
                ItemVO.builder().id(1L).email("green@green.com").price(123.0)
                        .title("Banany").amount(2).createdDate(new Date())
                        .build()
        ));
        futures.add(items.document("item-2").set(
                ItemVO.builder().id(2L).email("green@green.com").price(123.0)
                        .title("Banany").amount(2).createdDate(new Date())
                        .build()
        ));
        List<WriteResult> writeResults = ApiFutures.allAsList(futures).get();
        writeResults.forEach(o -> log.info("Updated time: {}", o.getUpdateTime()));
    }


    private Object converter(String data) throws IOException {
        return new ObjectMapper().readValue(data, FirebaseResponse.class);
    }

    protected String getSaltString() {
        String SALTCHARS = "1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 4) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }

    protected String getSaltStringChat() {
        String SALTCHARS = "1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 4) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }

    protected String getIncrementalChat() {
        int awal = 0;
        awal++;
        return awal + "";
    }
}

