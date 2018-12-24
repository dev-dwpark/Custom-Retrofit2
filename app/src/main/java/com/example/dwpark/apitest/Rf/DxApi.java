package com.example.dwpark.apitest.Rf;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.dwpark.apitest.Model.ApiModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by dwp on 2017. 5. 31..
 */

public class DxApi extends AsyncTask<RfFactory.Call_<?>, Void, Void> {
    private static final String DOMAIN = "http://192.168.12.178";
    //    private static final String DOMAIN = "http://www.digitalworks.co.kr";
    private static final String TAG = "DxApi";
    private static final String API_DEFAULT_VER = "0.0.0";
    private static final String RESULT_ERROR = "99";
    private static final String RESULT_NETWORK_ERROR = "98";
    private static final String MSG_NETWORK_ERROR = "network error";
    private static final String RESULT_CAST_EXCEPTION = "97";
    private static final int TIMEOUT = 15;

    private static ApiModel apiModel;
    private static Retrofit rf;

    private static String headerString = "";

    private RfFactory.Call_<?> call;
    private onApiCallback callback;
    private String weakCallback, weakClsName;

    /**
     * @author dwpark
     * @param headers
     * @return
     * @see -> set header (clear->reset)
     */
    public static synchronized void resetHeader(HashMap<String, String> headers){
        //todo 작업 예정
        new DxApi().setHeaderString(new DxApi().getKeyValueForm(headers));
    }

    /**
     * @author dwpark
     * @param header
     * @return
     * @see -> set header (clear->reset)
     */
    public static synchronized void resetHeader(String header){
        //todo 작업 예정
        new DxApi().setHeaderString(header);
    }

    /**
     * @author dwpark
     * @return
     * @see -> clear header
     */
    public static synchronized void clearHeader(){
        new DxApi().setHeaderString("");
    }

    public synchronized String getHeaderString(){
        return headerString;
    }

    public synchronized void setHeaderString(String headerString) {
        this.headerString = headerString;
    }

    /**
     * @author dwpark
     * @see -> api module create
     */
    private static void init(){
        OkHttpClient.Builder http = new OkHttpClient.Builder()
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .method(original.method(), original.body())
                                .addHeader("User-Agent", "AOS")
                                .addHeader("asd", new DxApi().getHeaderString())
                                .build();
                            //.headers

                        return chain.proceed(request);
                    }
                });


        Gson gson = new GsonBuilder().serializeNulls().create();

        rf = new Retrofit.Builder()
                .baseUrl(DOMAIN)
                .addCallAdapterFactory(new RfFactory.CallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(http.build())
                .build();

        apiModel = rf.create(ApiModel.class);
    }

    /**
     * @author dwpark
     * @return -> api model
     * @see -> get api model
     */
    public static ApiModel rf(){
        if(rf==null || apiModel==null){
            init();
        }
        return apiModel;
    }

    public class Calling{

        /**
         * @author dwpark
         * @param callback -> strong callback (listener)
         * @param params -> model call
         */
        public void async(onApiCallback callback, RfFactory.Call_<?>... params){
            DxApi.this.weakCallback = null;
            DxApi.this.callback = callback;
            DxApi.this.execute(params);
        }

        /**
         * @author dwpark
         * @param weakCallback -> weak callback (string)
         * @param params -> model call
         */
        public void async(String weakCallback, RfFactory.Call_<?>... params){
            StackTraceElement[] ste = new Throwable().getStackTrace();

            if(ste.length>1){
                weakClsName = ste[2].getClassName();
            }

            DxApi.this.callback = null;
            DxApi.this.weakCallback = weakCallback;
            DxApi.this.execute(params);
        }

        /**
         * @author dwpark
         * @param headerString
         * @return
         * @see -> ; 구분자는 허용하지 않는다.
         */
        public Calling addHeader(String headerString){
            headerString = headerString.replace(";", "");
            if(isKeyValueForm(headerString)){
                headerString = ";"+headerString;
                new DxApi().setHeaderString(getHeaderString()+headerString);
                Log.d("pppdw", "header true-"+getHeaderString());
            }else{
                Log.d("pppdw", "header false-"+headerString);
            }
            return getCalling();
        }

        /**
         * @author dwpark
         * @param headers
         * @return
         * @see -> 맵 형식의 헤더를 전체 Add
         */
        public Calling addHeader(HashMap<String, String> headers){
            new DxApi().setHeaderString(getHeaderString()+getKeyValueForm(headers));
            return getCalling();
        }
    }


    public interface onApiCallback {
        void result(Object obj);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected synchronized Void doInBackground(RfFactory.Call_<?>... params) {
        call = params[0];
        callback(call);
        return null;
    }

    /**
     * @author dwpark
     * @param call -> call api
     * @param <T>
     */
    public synchronized <T> void callback(final RfFactory.Call_<T> call) {

        final Handler handler = new Handler(Looper.getMainLooper());

        call.async(new RfFactory.MyCallback<T>() {
            @Override
            public void success(final Response<T> response) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        int code = response.code();
                        if (code >= 200 && code < 300) {
                            Log.i(TAG, "[[[DxApi-Success]]]");
                            try {
                                succ(response);
                            } catch (ClassCastException ce) {
                                Log.e(TAG, "!!![[[ClassCastException]] "+ce.toString()+"!!!");
                                castExceptionDisposal(RESULT_CAST_EXCEPTION, ce.toString(), ce.toString());
                            } catch (Exception e) {
                                Log.e(TAG, "!!![[[Exception]] "+e.toString()+"!!!");
                                occurredCastExceptionDisguisedThrows(RESULT_CAST_EXCEPTION, e.getCause().toString(), e);
                            }
                        }else {
                            if (code == 401) {
                                Log.w(TAG, "[[[DxApi-Unauthenticated]]]");
                            } else if (code >= 400 && code < 500) {
                                Log.w(TAG, "[[[DxApi-ClientError]]]");
                            } else if (code >= 500 && code < 600) {
                                Log.w(TAG, "[[[DxApi-ServerError]]]");
                            } else {
                                Log.w(TAG, "[[[DxApi-UnexpectedError]]]");
                            }

                            try {
                                fail(response);
                            } catch (ClassCastException ce) {
                                Log.e(TAG, "!!![[[ClassCastException]] "+ce.toString()+"!!!");
                                castExceptionDisposal(RESULT_CAST_EXCEPTION, ce.toString(), ce.toString());
                            } catch (Exception e) {
                                Log.e(TAG, "!!![[[Exception]] "+e.toString()+"!!!");
                                occurredCastExceptionDisguisedThrows(RESULT_CAST_EXCEPTION, e.getCause().toString(), e);
                            }
                        }
                    }
                });
            }

            @Override
            public void failur(final Throwable t) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (t instanceof IOException) {
                            Log.w(TAG, "[[[DxApi-NetworkError]]]");
                            error(RESULT_NETWORK_ERROR, MSG_NETWORK_ERROR);
                        } else {
                            Log.w(TAG, "[[[DxApi-UnexpectedError]]]");
                            error(RESULT_ERROR, t.getMessage());
                        }
                    }
                });
            }
        });
    }

    /**
     * @author dwpark
     * @param code -> error code
     * @param msg -> error msg
     * @param e -> throwable 이 classCastException 으로 인해 발생 된 것 인가?
     */
    private synchronized void occurredCastExceptionDisguisedThrows(String code, String msg, Exception e){
        Throwable t = e.getCause();
        if(t==null){
            return;
        }

        String cause = t.toString();
        if(cause.contains("ClassCastException")){
            Log.w(TAG, "!!![[[occurredCastExceptionDisguisedThrows]] "+cause.toString()+"!!!");
            castExceptionDisposal(code, msg, cause.toString());
        }
    }

    /**
     * @author dwpark
     * @param code -> error code
     * @param msg -> error msg
     * @param e -> com.pkg.className cannot be cast to com.pkg.className 의 규격으로 리턴 되는 classCastException 에서 타겟 class를 잡기 위해.
     */
    private synchronized void castExceptionDisposal(String code, String msg, String e){
        String[] invalidCastClass = e.split(" cannot be cast to ");
        try {
            castError(setErrorData(code, msg, invalidCastClass[1].trim()));
        } catch (Exception e1) {
            Log.e(TAG, "!!![[[Exception]] "+e+"!!!");
        }
    }

    /**
     * @author dwpark
     * @param response
     * @param <T>
     * @return -> create data model object
     */
    public final <T> Object findCastForObj (Response<T> response) {
        return response.body();
    }

    /**
     * @author dwpark
     * @param obj default data model
     * @throws Exception 기타 Exception 처리
     */
    private synchronized void castError(Object obj) throws Exception{
        if(callback!=null){
            calledStrongCallback(obj);
        }else if(weakCallback!=null && !weakCallback.isEmpty()){
            calledWeakCallback(obj);
        }
    }

    /**
     * @author dwpark
     * @param response
     * @throws Exception -> 기타 Exception 처리
     */
    private synchronized void succ(Response response) throws Exception{
        if(callback!=null){
            calledStrongCallback(findCastForObj(response));
        }else if(weakCallback!=null && !weakCallback.isEmpty()){
            try {
                calledWeakCallback(findCastForObj(response));
            } catch (ClassCastException ce) {
                Log.e(TAG, "!!![[[ClassCastException]] "+ce.toString()+"!!!");
                castExceptionDisposal(RESULT_CAST_EXCEPTION, ce.toString(), ce.toString());
            } catch (Exception e) {
                Log.e(TAG, "!!![[[Exception]] "+e.toString()+"!!!");
                occurredCastExceptionDisguisedThrows(RESULT_CAST_EXCEPTION, e.getCause().toString(), e);
            }
        }
    }

    /**
     * @author dwpark
     * @param response
     * @throws Exception -> default data model 생성 시 classCastException 처리 목적 및 기타 Exception 처리
     */
    private synchronized void fail(Response response) throws Exception{
        try {
            if(callback!=null){
                calledStrongCallback(setErrorData(String.valueOf(response.code()), response.message(), Data.class.getName()));
            }else if(weakCallback!=null && !weakCallback.isEmpty()){
                calledWeakCallback(setErrorData(String.valueOf(response.code()), response.message(), Data.class.getName()));
            }
        } catch (ClassCastException e){
            Log.e(TAG, "!!![[[ClassCastException]] "+e.toString()+"!!!");
            castExceptionDisposal(String.valueOf(response.code()), response.message(), e.toString());
        } catch (Exception e) {
            Log.e(TAG, "!!![[[Exception]] "+e.toString()+"!!!");
            occurredCastExceptionDisguisedThrows(String.valueOf(response.code()), response.message(), e);
        }
    }

    /**
     * @author dwpark
     * @param code
     * @param msg
     * @throws Exception -> default data model 생성 시 classCastException 처리 목적 및 기타 Exception 처리
     */
    private synchronized void error(String code, String msg){
        try {
            if(callback!=null){
                calledStrongCallback(setErrorData(code, msg, Data.class.getName()));
            }else if(weakCallback!=null && !weakCallback.isEmpty()){
                calledWeakCallback(setErrorData(code, msg, Data.class.getName()));
            }
        } catch (ClassCastException e){
            Log.e(TAG, "!!![[[ClassCastException]] "+e.toString()+"!!!");
            castExceptionDisposal(code, msg, e.toString());
        } catch (Exception e) {
            Log.e(TAG, "!!![[[Exception]] "+e.toString()+"!!!");
            occurredCastExceptionDisguisedThrows(code, msg, e);
        }
    }

    /**
     * @author dwpark
     * @param code -> custom error code / response error code
     * @param msg -> custom error msg / response error msg
     * @param castName -> 해당 모델이 캐스트 되어야 하는 클래스 네임
     * @return -> default data model
     */
    private Object setErrorData(String code, String msg, String castName){
        if(castName!=null){
            try {
                /** default data model create (reflection) **/
                Class<? extends Data> cls = (Class<? extends Data>) Class.forName(castName);
                Object obj = cls.newInstance();
                Method[] methods = obj.getClass().getMethods();
                for(Method method : methods){
                    method.setAccessible(true);
                    String methodName = method.getName();
                    if(methodName!=null && methodName.toUpperCase().contains(GSON_RESULT_CODE.toUpperCase())){
                        method.invoke(obj, new Object[] {code});
                    }else if(methodName!=null && methodName.toUpperCase().contains(GSON_API_NAME.toUpperCase())){
                        method.invoke(obj, new Object[] {msg});
                    }else if(methodName!=null && methodName.toUpperCase().contains(GSON_API_VERSION.toUpperCase())){
                        method.invoke(obj, new Object[] {API_DEFAULT_VER});
                    }
                }
                return obj;
            } catch (InvocationTargetException e) {
                Log.e(TAG, "!!![[[InvocationTargetException]] "+e.toString()+"!!!");
                return null;
            } catch (IllegalAccessException e) {
                Log.e(TAG, "!!![[[IllegalAccessException]] "+e.toString()+"!!!");
                return null;
            } catch (InstantiationException e) {
                Log.e(TAG, "!!![[[InstantiationException]] "+e.toString()+"!!!");
                return null;
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "!!![[[ClassNotFoundException]] "+e.toString()+"!!!");
                return null;
            }
        }else {
            Log.e(TAG, "!!![[[ErrorDataCreateFailed]] castClassName is null !!!");
            return null;
        }
    }

    /**
     * @author dwpark
     * @param obj
     */
    private void calledStrongCallback(Object obj){
        if(obj==null){
            Log.w(TAG, "!!![[[calledStrongCallback]]] return(or create) obj is null!!!");
            return;
        }
        callback.result(obj);
    }

    /**
     * @author dwpark
     * @param obj -> return object
     * @throws Exception -> classCastException 발생 시, obj->default data model
     */
    private void calledWeakCallback(Object obj) throws Exception{
        if(obj==null){
            Log.w(TAG, "!!![[[calledWeakCallback]]] return(or create) obj is null!!!");
            return;
        }

        if(weakClsName!=null && !weakClsName.isEmpty()){
            Class<?> cls = Class.forName(weakClsName);
            Object targetContext = cls.getConstructor().newInstance();
            Class parameters[] = new Class[]{Object.class};
            Method method = cls.getMethod(weakCallback, parameters);
            method.setAccessible(true);
            Object objs[] = new Object[1];
            objs[0] = obj;
            method.invoke(targetContext, objs);
        }
    }

    /**
     * @author dwpark
     * @param keyVal -> kay=value 형식의 스트링 데이터
     * @return
     * @see -> addHeaderString 에서 헤더 폼 체크에 사용
     */
    private boolean isKeyValueForm(String keyVal){
        keyVal = keyVal.replace(" ", "");
        return Pattern.matches("[\\w\\~\\-\\.]+=[\\w\\~\\-]+", keyVal.trim());
    }

    /**
     * @author dwpark
     * @param map
     * @return
     * @see -> 해쉬 맵 데이터를 key=val;key1=val1;.......keyn=valn 의 형태로 리턴
     */
    private String getKeyValueForm(HashMap<String, String> map){
        String keyVal = "";
        if(map!=null && map.size()>0){
            Set key = map.keySet();
            for (Iterator iterator = key.iterator(); iterator.hasNext();) {
                String keyName = (String) iterator.next();
                String valueName = map.get(keyName);
                String temp = ";"+keyName+"="+valueName;
                keyVal += temp;
            }
        }
        return keyVal;
    }

    public Calling getCalling(){
        return new Calling();
    }

    /********************************************************************************************/
    /** Default Data Model **********************************************************************/
    /********************************************************************************************/
    private static final String GSON_RESULT_CODE = "result_code";
    private static final String GSON_API_VERSION = "api_version";
    private static final String GSON_API_NAME = "api_name";
    /**
     * @author dwpark
     * @see default data model class
     */
    public static class Data {
        @SerializedName(GSON_RESULT_CODE) private String result_code;
        @SerializedName(GSON_API_VERSION) private String api_version;
        @SerializedName(GSON_API_NAME) private String api_name;

        public String getResultCode() {
            return result_code;
        }

        public String getApiVersion() {
            return api_version;
        }

        public String getApiName() {
            return api_name;
        }

        public void setResult_code(String result_code) {
            this.result_code = result_code;
        }

        public void setApi_version(String api_version) {
            this.api_version = api_version;
        }

        public void setApi_name(String api_name) {
            this.api_name = api_name;
        }
    }
    /********************************************************************************************/
    /** Default Data Model **********************************************************************/
    /********************************************************************************************/
}