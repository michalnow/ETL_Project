package com.etl;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Parser {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Map<String,List<Element>> opinionBody = null;
        List<Element> phonesBody = null;
        Map<String,List<Opinion>> opinions = null;
        List<Phone> phones = null;
        String choice = "";

        do {
            System.out.println("Command Options: ");
            System.out.println("1 - Extract");
            System.out.println("2 - Transform");
            System.out.println("3 - Load");
            System.out.println("q: Quit");
            choice = scanner.nextLine();
            switch (choice){
                case "1":
                    Document document = null;
                    try {
                        document = Jsoup.connect("https://www.ceneo.pl/Smartfony").get();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    assert document != null;

                    System.out.println("Extract !!!");

                    List<String> phoneUrls = preparePhoneUrls(document);
                    List<String> urls = prepareOpinionUrls(phoneUrls);
                    System.out.println(urls);
                    phonesBody = extractsPhoneHtml(phoneUrls);
                    opinionBody = extractOpinionHtml(urls);
                    System.out.println("Data has been extracted");
                    break;

                case "2":
                    System.out.println("\n\nTransform !!!");
                    phones = generatePhones(phonesBody);
                    opinions = generateOpinions(opinionBody);
                    System.out.println("Data has been transformed");
                    break;

                case "3":
                    System.out.println("\n\nLoad !!!");
                    loadPhonesToDb(phones);
                    loadOpinionsToDb(opinions);
                    System.out.println("Data has been loaded to db");
                    break;
            }

        }while (!choice.equals("q"));

    }

    private static void loadPhonesToDb(List<Phone> phones){
        String postUrl = "http://localhost:8080/api/phone";
        for(Phone phone: phones){
            StringEntity postingString;
            try {
                Gson gson = new Gson();
                postingString = new StringEntity(gson.toJson(phone), "UTF-8");
                postingString.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,
                        "application/json;charset=UTF-8"));
                System.out.println(gson.toJson(phone));
                HttpClient httpClient = HttpClientBuilder.create().build();

                HttpPost httpPost = new HttpPost(postUrl);
                httpPost.setHeader("Content-type","application/json;charset=UTF-8");
                httpPost.setEntity(postingString);

                HttpResponse response = httpClient.execute(httpPost);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void loadOpinionsToDb(Map<String,List<Opinion>> opinions) {
        String postUrl = "http://localhost:8080/api/opinion/";
        int i = 0;
        Set<String> keys = opinions.keySet();
        System.out.println(keys);

        for(String key: keys) {
            i++;
            System.out.println("key = " + key);
            List<Opinion> opinionList = opinions.get(key);
            for (Opinion opinion : opinionList) {
                StringEntity postingString;
                try {
                    Gson gson = new Gson();
                    postingString = new StringEntity(gson.toJson(opinion), "UTF-8");
                    postingString.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,
                            "application/json;charset=UTF-8"));
                    System.out.println(gson.toJson(opinion));
                    HttpClient httpClient = HttpClientBuilder.create().build();

                    HttpPost httpPost = new HttpPost(postUrl + i);
                    System.out.println(postUrl+i);
                    httpPost.setHeader("Content-type", "application/json;charset=UTF-8");
                    httpPost.setEntity(postingString);

                    HttpResponse response = httpClient.execute(httpPost);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static List<String> prepareOpinionUrls(List<String> phoneUrls)  {
        Set<String> urls = new LinkedHashSet<>();
        Document document = null;
        String first = "opinie-1";

        for (String phoneUrl : phoneUrls) {
            try {
                document = Jsoup.connect(phoneUrl).get();
            } catch (IOException e) {
                e.printStackTrace();
            }

            int j = 0;
            String next = "";

            while (true) {
                if (j == 0) {
                    String href = document.select("li.arrow-next").select("a").attr("href");
                    next = href.substring(0, 10) + first;
                } else {
                    assert document != null;
                    next = document.select("li.arrow-next").select("a").attr("href");
                }
                System.out.println(next);
                if (next.equals("")) {
                    break;
                }

                String url = "https://www.ceneo.pl" + next;
                urls.add(url);
                j++;

                try {
                    document = Jsoup.connect(url).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        System.out.println(urls);
        return urls.stream().collect(Collectors.toList());
    }

    private static List<String> preparePhoneUrls(Document document){
        Set<String> phoneUrls = new LinkedHashSet<>();
        Elements links = document.select("a.go-to-product");
        Element link = null;
        int i = 0;

        while(i < links.size()){ // links.size();
            link = links.get(i);
            String phone = link.attr("href") + "#tab=reviews";
            String phoneUrl = "https://ceneo.pl" + phone;
        //    System.out.println(phoneUrl);
            phoneUrls.add(phoneUrl);
            i++;
        }

        System.out.println(links.size());
        List<String> urls = phoneUrls.stream().collect(Collectors.toList());
        return urls;

    }

    private static List<Element> extractsPhoneHtml(List<String> phoneUrls){
        List<Element> phonesBody = new ArrayList<>();
        Document document = null;

        for (String url: phoneUrls){
            try {
                document = Jsoup.connect(url).get();
            } catch (IOException e) {
                e.printStackTrace();
            }

            for(Element el: document.select("table.product-content")){
                phonesBody.add(el);
            }
        }

        return phonesBody;
    }

    private static Map<String,List<Element>> extractOpinionHtml(List<String> urls){
        Map<String,List<Element>> opinionsHtml = new LinkedHashMap<>();
        List<Element> reviewsBody = null;
        Document document = null;

        for (String url : urls) {
            if(opinionsHtml.get(url.substring(21,29)) == null){
                reviewsBody = new ArrayList<>();
            }
            System.out.println(url);
            try {
                document = Jsoup.connect(url).get();
            } catch (IOException e) {
                e.printStackTrace();
            }

            assert document != null;
            for(Element el: document.select("li.review-box")){
                //System.out.println(el);
                reviewsBody.add(el);
            }

            opinionsHtml.put(url.substring(21,29),reviewsBody);
            System.out.println(reviewsBody.size());
        }

        return opinionsHtml;
    }

    private static List<Phone> generatePhones(List<Element> phonesBody){
        List<Phone> phones = new ArrayList<>();

        for(Element phoneHtml: phonesBody){
            Phone phone = new Phone();
            phone.setFullName(phoneHtml.select("h1.product-name").text());
            phone.setDescription(phoneHtml.select("div.ProductSublineTags").text());
            phones.add(phone);
        }

        return phones;
    }

    private static Map<String,List<Opinion>> generateOpinions(Map<String,List<Element>> reviewsBody) {
        Map<String,List<Opinion>> opinionReady = new LinkedHashMap<>();
        Set<String> keys = reviewsBody.keySet();
        List<Opinion> opinions;
        int i = 0;
        System.out.println(keys);

            for(String key: keys) {
                i++;
                opinions = new ArrayList<>();
                for (Element review : reviewsBody.get(key)) {
                    Opinion opinion = new Opinion();
                    opinion.setNickname(review.select("div.reviewer-name-line").text());
                    opinion.setRecommendation(review.select("em.product-recommended").text()
                            .equals("Polecam") ? "Tak" : "Nie");

                    String grade = review.select("span.review-score-count").text();
                    opinion.setGrade(!grade.equals("") ? Integer.parseInt(grade.substring(0, 1)) : 0);
                    opinion.setReview(review.select("p.product-review-body").text());

                    String thumbsUp = review.select("button.vote-yes").attr("data-total-vote");
                    String thumbsDown = review.select("button.vote-no").attr("data-total-vote");
                    opinion.setThumbsUp(Integer.parseInt(!thumbsUp.equals("") ? thumbsUp : "0"));
                    opinion.setThumbsDown(Integer.parseInt(!thumbsDown.equals("") ? thumbsDown : "0"));

                    String date;
                    date = review.select("time").attr("datetime").substring(0, 10);
                    opinion.setPublishDate(date);
                    opinion.setPhone_id((long) i);
                    opinions.add(opinion);
                    System.out.println(opinion);
                }
                 opinionReady.put(key, opinions);
            }

            Set<String> keyset = opinionReady.keySet();
            for(String key: keyset){
                System.out.println("keeeeey " + key);
                System.out.println(opinionReady.get(key));
            }

        return opinionReady;
    }
}