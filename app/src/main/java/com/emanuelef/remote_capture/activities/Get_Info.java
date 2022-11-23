package com.emanuelef.remote_capture.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.model.GetInfo_DTO;
import com.emanuelef.remote_capture.pcap_dump.UDPDumper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


public class Get_Info extends AppCompatActivity implements Runnable {
    List<GetInfo_DTO> items;
    ListView list;
    Button load_btn;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.get_info);

        load_btn = findViewById(R.id.load_btn);
        list = (ListView) findViewById (R.id.list);

        items = new ArrayList<>();

        // 백그라운드 스레드
        load_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread th = new Thread(Get_Info.this);
                th.start();
            }
        });
        }


    // 핸들러
    @SuppressLint("HandlerLeak")
    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String[] str =new String[items.size()];
            for(int i=0; i<str.length; i++){
                GetInfo_DTO dto =items.get(i);
                str[i]=(i+1)+". "+ dto.getAppName()+"\n [ " + dto.getLeakType()+" ] " + "\n [ " + dto.getDateNTime() + " ] " ;
            }

            // 안드로이드가 미리 만들어놓은 simple_list_item_1 레이아웃으로 어댑터 생성(텍스트뷰 하나로 구성)
            ArrayAdapter<String> adapter=new ArrayAdapter<String>(Get_Info.this, android.R.layout.simple_list_item_1, str);
            list.setAdapter(adapter);
        }

    };

    @Override
    public void run() {
        try {
            StringBuffer sb = new StringBuffer();
            URL url = new URL("http://13.125.192.215:1235/user/leak?phoneNum="+ UDPDumper.Phone);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 저 경로의 source를 받아온다.
            if (conn != null) {
                conn.setConnectTimeout(5000);
                conn.setUseCaches(false);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                    while (true) {
                        String line = br.readLine();
                        if (line == null)
                            break;
                        sb.append(line + "\n");
                    }
                    Log.d("myLog", sb.toString());
                    br.close();
                }
                conn.disconnect();
            }

            // 받아온 source를 JSONObject로 변환한다.
            JSONObject jsonObj = new JSONObject(sb.toString());
            Log.d("GET DATA", String.valueOf(jsonObj));
            JSONArray jArray = (JSONArray) jsonObj.get("LeakAppDto");
            for(int i = 0 ; i < jArray.length() ; i++) {
                JSONObject row = jArray.getJSONObject(i);
                GetInfo_DTO dto = new GetInfo_DTO();
                dto.setAppName(row.getString("appName"));
                dto.setLeakType(row.getString("leakType"));
                dto.setDateNTime(Timestamp.valueOf(row.getString("dateNTime")));
                items.add(dto);
            }

        }catch (Exception e){
            e.printStackTrace();
            Log.e("error", e.getMessage());
        }
        //핸들러에게 메시지 요청
        handler.sendEmptyMessage(0);
    }
}
