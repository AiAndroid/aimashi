public class GsonRequest<T> extends Request<T> {
        private final Gson gson = new Gson();
        private final Type type;
        private final Map<String, String> headers;
        private final Response.Listener<T> listener;
        private String cacheFile;

        public void setCacheNeed(String _cacheFile){
            cacheFile = _cacheFile;
        }

        public GsonRequest(String url, Type type, Map<String, String> headers,
                           Response.Listener<T> listener, Response.ErrorListener errorListener) {
            super(Method.GET, url, errorListener);
            this.type = type;
            this.headers = headers;
            this.listener = listener;
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return headers != null ? headers : super.getHeaders();
        }

        @Override
        protected void deliverResponse(T response) {
            listener.onResponse(response);
        }

        @Override
        protected Response<T> parseNetworkResponse(NetworkResponse response) {
            try {
                String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                Log.d(TAG, "response json:" + json);
                long timeStart = System.currentTimeMillis();
                T fromJson = gson.fromJson(json, type);
                long timeEnd = System.currentTimeMillis();
                Log.d(TAG, "fromJson take time in ms: " + (timeEnd - timeStart));
                Response<T> res =  Response.success(fromJson, HttpHeaderParser.parseCacheHeaders(response));

                if(mEnableCache && cacheFile != null && cacheFile.length() > 0){
                    //save to files
                    updateToFile(cacheFile, json);
                }
                return  res;
            } catch (UnsupportedEncodingException e) {
                return Response.error(new ParseError(e));
            } catch (JsonSyntaxException e) {
                return Response.error(new ParseError(e));
            }
        }
    }
