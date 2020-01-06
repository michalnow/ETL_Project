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

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Parser {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Map<String,List<Element>> opinionBody = null;
        List<Element> phonesBody = null;
        Map<String,List<Opinion>> opinions = null;
        List<Phone> phones = null;
        String choice;

        do {
            System.out.println("Command Options: ");
            System.out.println("1 - Extract");
            System.out.println("2 - Transform");
            System.out.println("3 - Load");
            System.out.println("4 - Export to CSV");
            System.out.println("5 - Whole ETL Process");
            System.out.println("q: Quit");
            choice = scanner.nextLine();
            switch (choice){
                case "1":
                    String url = "";
                    Document document = null;

                    while(!url.equals("ceneo.pl/Smartfony")){
                        System.out.println("Enter valid link to smartphones without https://www.");
                        url = scanner.nextLine();
                    }
                    try {
                        document = Jsoup.connect("https://www." + url).get();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    assert document != null;

                    System.out.println("\nEXTRACT !!!");

                    List<String> phoneUrls = preparePhoneUrls(document);
                    List<String> urls = prepareOpinionUrls(phoneUrls);
                    phonesBody = extractsPhoneHtml(phoneUrls);
                    opinionBody = extractOpinionHtml(urls);
                    System.out.println("Number of sites extracted with phones = " + phonesBody.size());
                    System.out.println("Number of sites extracted with opinions = " + opinionBody.size());
                    System.out.println("\nDATA HAS BEEN EXTRACTED");
                    break;

                case "2":
                    System.out.println("\nTRANSFORM !!!");
                    phones = generatePhones(phonesBody);
                    opinions = generateOpinions(opinionBody);
                    System.out.println("Number of phones = " + phones.size());
                    System.out.println("Number of opinions = " + opinions.size());
                    System.out.println("\nDATA HAS BEEN TRANSFORMED");
                    break;

                case "3":
                    System.out.println("\nLOAD !!!");
                    loadPhonesToDb(phones);
                    loadOpinionsToDb(opinions);
                    System.out.println("Number of phones loaded to DB = " + phones.size());
                    System.out.println("\nDATA HAS BEED LOADED TO DB");
                    break;
                case "4":
                    System.out.println("\nEXPORT TO CSV !!!");
                    exportToCsv(opinions,phones);
                    System.out.println("\nDATA HAS BEED EXPORTED TO CSV");
                    break;
                case "5":
                    System.out.println("Whole ETL PROCESS !");
                    String url2 = "";
                    Document document2 = null;

                    while(!url2.equals("ceneo.pl/Smartfony")){
                        System.out.println("Enter valid link to smartphones without https://www.");
                        url2 = scanner.nextLine();
                    }
                    try {
                        document2 = Jsoup.connect("https://www." + url2).get();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    assert document2 != null;

                    System.out.println("\nEXTRACT !!!");
                    List<String> phoneUrls2 = preparePhoneUrls(document2);
                    List<String> urls2 = prepareOpinionUrls(phoneUrls2);
                    phonesBody = extractsPhoneHtml(phoneUrls2);
                    opinionBody = extractOpinionHtml(urls2);
                    System.out.println("Number of sites extracted with phones = " + phonesBody.size());
                    System.out.println("\nDATA HAS BEEN EXTRACTED");

                    System.out.println("\nTRANSFORM !!!");
                    phones = generatePhones(phonesBody);
                    opinions = generateOpinions(opinionBody);
                    System.out.println("\nDATA HAS BEEN TRANSFORMED");

                    System.out.println("\nLOAD !!!");
                    loadPhonesToDb(phones);
                    loadOpinionsToDb(opinions);
                    System.out.println("\nDATA HAS BEED LOADED TO DB");
                    break;
            }

        }while (!choice.equals("q"));

    }

    private static void loadPhonesToDb(List<Phone> phones){
        int count = 0;
        String postUrl = "http://localhost:8080/server/api/phone";
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
                System.out.println("EXECUTING POST " + postUrl);
                httpPost.setHeader("Content-type","application/json;charset=UTF-8");
                httpPost.setEntity(postingString);

                HttpResponse response = httpClient.execute(httpPost);
                count++;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Number of phones loaded to DB = " + count);
    }

    private static void loadOpinionsToDb(Map<String,List<Opinion>> opinions) {
        String postUrl = "http://localhost:8080/server/api/opinion/";
        int count = 0;
        Set<String> keys = opinions.keySet();
        System.out.println(keys);

        for(String key: keys) {
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

                    HttpPost httpPost = new HttpPost(postUrl + key);
                    System.out.println("EXECUTING POST " + postUrl +key);
                    httpPost.setHeader("Content-type", "application/json;charset=UTF-8");
                    httpPost.setEntity(postingString);

                    HttpResponse response = httpClient.execute(httpPost);
                    count++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Number of opinions loaded to DB = " + count);
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

                if (next.equals("")) {
                    break;
                }

                String url = "https://www.ceneo.pl" + next;
                System.out.println("Generated url = " + url);
                urls.add(url);
                j++;

                try {
                    document = Jsoup.connect(url).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        System.out.println("\nURLS ARE READY");
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
            phoneUrls.add(phoneUrl);
            i++;
        }

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

            for(Element el: document.select("div.product")){
                phonesBody.add(el);
            }
        }
        System.out.println("Number of phones extracted = " + phonesBody.size());
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

            try {
                document = Jsoup.connect(url).get();
            } catch (IOException e) {
                e.printStackTrace();
            }

            assert document != null;
            for(Element el: document.select("li.review-box")){
                reviewsBody.add(el);
            }

            opinionsHtml.put(url.substring(21,29),reviewsBody);

        }

        Set<String> keys = opinionsHtml.keySet();
        for(String key: keys){
            System.out.println("num of extracted opinions for phone with ID " + key + " = " + opinionsHtml.get(key).size());
        }

        return opinionsHtml;
    }

    private static List<Phone> generatePhones(List<Element> phonesBody){
        List<Phone> phones = new ArrayList<>();

        for(Element phoneHtml: phonesBody){
            Phone phone = new Phone();
            phone.setPhone_id(phoneHtml.select("span.context-menu").attr("data-pid"));
            phone.setFullName(phoneHtml.select("table.product-content").select("h1.product-name").text());
            phone.setDescription(phoneHtml.select("table.product-content").select("div.ProductSublineTags").text());
            phone.setImageUrl(phoneHtml.select("div.product-carousel").attr("content"));
            phones.add(phone);
        }

        System.out.println("Number of transformed phones = " + phones.size());

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
                    opinion.setId(review.select("li.review-box").attr("data-entry-id"));
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
                    opinion.setPhone_id(key);
                    opinions.add(opinion);
                    System.out.println(opinion);
                }
                 opinionReady.put(key, opinions);
            }

            Set<String> keyset = opinionReady.keySet();
            for(String key: keyset){
                System.out.println("\nCENEO PHONE ID = " + key);
                System.out.println("Number of opinions = " + opinionReady.get(key).size());
            }

        return opinionReady;
    }

    private static void exportToCsv(Map<String, List<Opinion>> opinions, List<Phone> phones){
        String csvSeperator = ";";
        Set<String> keys = opinions.keySet();

        try
        {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("opinions.csv"), "UTF-8"));
            for (String key : keys){
                List<Opinion> ops = opinions.get(key);
                for(Opinion opinion: ops){
                    StringBuffer oneLine = new StringBuffer();
                    oneLine.append(opinion.getId());
                    oneLine.append(csvSeperator);
                    for(Phone phone: phones){
                        System.out.println(phone);
                        System.out.println(opinion);
                        System.out.println(phone.getPhone_id());
                        if(phone.getPhone_id().equals(opinion.getPhone_id())) {
                            oneLine.append(phone.getFullName());
                            break;
                        }
                    }
                    oneLine.append(csvSeperator);
                    oneLine.append(opinion.getNickname().trim());
                    oneLine.append(csvSeperator);
                    oneLine.append(opinion.getReview());
                    oneLine.append(csvSeperator);
                    oneLine.append(opinion.getGrade());
                    oneLine.append(csvSeperator);
                    oneLine.append(opinion.getPublishDate());
                    oneLine.append(csvSeperator);
                    oneLine.append(opinion.getThumbsUp());
                    oneLine.append(csvSeperator);
                    oneLine.append(opinion.getThumbsDown());
                    oneLine.append(csvSeperator);
                    oneLine.append(opinion.getRecommendation());
                    oneLine.append(csvSeperator);

                    bw.write(oneLine.toString());
                    bw.newLine();


                }
            }
            bw.flush();
            bw.close();
        } catch (IOException e) {}
    }
}