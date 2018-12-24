package com.example.dwpark.apitest.Rf;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by dwpark on 2017. 9. 25..
 * retrofit custom factory
 * example : api model- RfFactory.Call_<generic> name;
 *           create retrofit - .addCallAdapterFactory(new RfFactory.CallAdapterFactory())
 */

public final class RfFactory {
    private static final String TAG = "RfFactory";

    /**
     * @author dwpark
     * @param <T>
     * @see -> retrofit custom !!call!!
     */
    public interface Call_<T> {
        void cancel();
        Call_<T> clone();
        void async(MyCallback<T> callback);
        void async(String weakListener);
        void async(DxApi.onApiCallback callback);
        T sync();

        /** add Header (string) **/
        Call_<T> addHeader(String header);

        /** add Header (map) **/
        Call_<T> addHeader(HashMap<String, String> headers);


    }

    /**
     * @author dwpark
     * @param <T>
     * @see -> retrofit enqueue callback
     */
    public interface MyCallback<T> {
        void success(Response<T> response);
        void failur(Throwable t);
    }

    /**
     * @author dwpark
     * @see -> custom factory
     */
    public static class CallAdapterFactory extends CallAdapter.Factory {
        @Override
        public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
            if (getRawType(returnType) != Call_.class) {
                return null;
            }

            if (!(returnType instanceof ParameterizedType)) {
                //todo 필요에 따라서 throws
            }

            Type responseType = getParameterUpperBound(0, (ParameterizedType) returnType);
            Executor callbackExecutor = retrofit.callbackExecutor();
            return new CustomCallAdapter<>(responseType, callbackExecutor);
        }

        private static final class CustomCallAdapter<R> implements CallAdapter<R, Call_<R>> {
            private final Type responseType;
            private final Executor callbackExecutor;

            CustomCallAdapter(Type responseType, Executor callbackExecutor) {
                this.responseType = responseType;
                this.callbackExecutor = callbackExecutor;
            }

            @Override
            public Type responseType() {
                return responseType;
            }

            @Override
            public Call_<R> adapt(Call<R> call) {
                return new MyCallAdapter<>(call, callbackExecutor);
            }
        }
    }

    static class MyCallAdapter<T> implements Call_<T> {
        private final Call<T> call;
        private final Executor callbackExecutor;
        private final DxApi api_;

        MyCallAdapter(Call<T> call, Executor callbackExecutor) {
            this.call = call;
            this.callbackExecutor = callbackExecutor;
            this.api_ = new DxApi();
        }

        @Override
        public void cancel() {
            call.cancel();
        }

        @Override
        public void async(final MyCallback<T> callback) {
            call.enqueue(new Callback<T>() {
                @Override
                public void onResponse(Call<T> call, Response<T> response) {
                    callback.success(response);
                }

                @Override
                public void onFailure(Call<T> call, Throwable t) {
                    callback.failur(t);
                }
            });
        }

        @Override
        public Call_<T> clone() {
            return new MyCallAdapter<>(call.clone(), callbackExecutor);
        }

        @Override
        public void async(String weakListener) {
            api_.getCalling().async(weakListener, this);
        }

        @Override
        public void async(DxApi.onApiCallback callback) {
            api_.getCalling().async(callback, this);
        }

        @Override
        public @Nullable T sync(){
            Response response = null;
            try {
                response = new CallSyncExecutor().execute(call).get();
            } catch (InterruptedException e) {
                Log.e(TAG, "!!![[[InterruptedException]] "+e.toString()+"!!!");
            } catch (ExecutionException e) {
                Log.e(TAG, "!!![[[ExecutionException]] "+e.toString()+"!!!");
            }

            if(response!=null){
                return (T) response.body();
            }else{
                Log.w(TAG, "[[[DxApi-Sync Fail]]]");
                return null;
            }

        }

        @Override
        public Call_<T> addHeader(String header) {
            api_.getCalling().addHeader(header);
            return this;
        }

        @Override
        public Call_<T> addHeader(HashMap<String, String> headers) {
            api_.getCalling().addHeader(headers);
            return this;
        }
    }

    /**
     * @author dwpark
     * @see -> sync 타입 콜은 innerclass로 관리한다.
     */
    private static class CallSyncExecutor extends AsyncTask<Call<?>, Void, Response>{
        @Override
        protected Response doInBackground(Call<?>... calls) {
            Call call = calls[0];
            try {
                return call.execute();
            } catch (IOException e) {
                Log.e(TAG, "!!![[[IOException]] "+e.toString()+"!!!");
                return null;
            }
        }
    }
}
