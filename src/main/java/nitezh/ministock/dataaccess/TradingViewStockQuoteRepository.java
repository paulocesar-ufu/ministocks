/*
 The MIT License

 Copyright (c) 2013 Nitesh Patel http://niteshpatel.github.io/ministocks

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

package nitezh.ministock.dataaccess;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import nitezh.ministock.domain.StockQuote;
import nitezh.ministock.utils.Cache;
import nitezh.ministock.utils.UrlDataTools;


public class TradingViewStockQuoteRepository {
    private static final String BASE_URL = "https://scanner.tradingview.com/symbol?fields=close,change&symbol=";

    private final FxChangeRepository fxChangeRepository;

    public TradingViewStockQuoteRepository(FxChangeRepository fxChangeRepository) {
        this.fxChangeRepository = fxChangeRepository;
    }

    public HashMap<String, StockQuote> getQuotes(Cache cache, List<String> symbols) {
        HashMap<String, StockQuote> quotes = new HashMap<>();
        JSONArray jsonArray;
        JSONObject quoteJson;

        try {
            jsonArray = this.retrieveQuotesAsJson(cache, symbols);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    quoteJson = jsonArray.getJSONObject(i);
                    StockQuote quote = new StockQuote(quoteJson.optString("symbol"), quoteJson.optString("price"), quoteJson.optString("change"), quoteJson.optString("percent"), quoteJson.optString("exchange"), quoteJson.optString("volume"), quoteJson.optString("name"), "", Locale.US);
                    quotes.put(quote.getSymbol(), quote);
                }
            }
        } catch (JSONException e) {
            return null;
        }

        return quotes;
    }

    JSONArray retrieveQuotesAsJson(Cache cache, List<String> symbols) throws JSONException {
        String url = BASE_URL;
        String quotesString = UrlDataTools.getCachedUrlData(url, symbols, cache, 5);
        JSONArray quotesJson = new JSONObject(quotesString).getJSONObject("quoteResponse").getJSONArray("result");

        return quotesJson;
    }
}
