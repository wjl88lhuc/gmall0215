package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ListServiceImpl implements ListService {

    public static final String ES_INDEX = "gmall";

    public static final String ES_TYPE = "SkuInfo";

    @Autowired
    private JestClient jestClient;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 保存数据到Elasticsearch中
     * 1. 定义动作
     * 2. 执行动作
     * 3.
     *
     * @param skuLsInfo
     */
    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        String query = makeQueryStringForSearch(skuLsParams);
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult = null;
        SkuLsResult skuLsResult = null;
        try {
            searchResult = jestClient.execute(search);
            skuLsResult = makeResultForSearch(skuLsParams, searchResult);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return skuLsResult;
        }
    }

    @Override
    public void incrHotScore(String skuId) {
        //获取redis客户端
        Jedis jedis = redisUtil.getJedisFromPool();
        //定义key
        String hotKey = "hotScore";
        Long count = jedis.zadd(hotKey, 1, "skuId:" + skuId);
        //按照一堆规则更新es
        if (count % 10 == 0 && jestClient != null){//每更新10次redis就更新es一次
            updateHotScore(skuId,  Math.round(count));
        }
    }

    private void updateHotScore(String skuId, int hotScore) {
        //1. 编写dsl语句
        String updateJson="{\n" +
                "   \"doc\":{\n" +
                "     \"hotScore\":"+hotScore+"\n" +
                "   }\n" + "}";

        //2. 定义动作
        Update update = new Update.Builder(updateJson).index("gmall").type("SkuInfo").id(skuId).build();

        //3. 执行
        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (jestClient != null){
                try {
                    jestClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }


    }

    /**
     * 设置返回结果
     *
     * @param skuLsParams
     * @param searchResult
     * @return
     */
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        //声明对象
        SkuLsResult skuLsResult = new SkuLsResult();
        //1. 给 SkuLsResult的skuLsInfoList 属性赋值
        List<SkuLsInfo> skuLsInfoList = new ArrayList<>();
        //从查询结果中把数据存入到集合中skuLsInfoList
        List<SearchResult.Hit<SkuLsInfo, Void>> hitList = searchResult.getHits(SkuLsInfo.class);
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hitList) {
            SkuLsInfo source = hit.source;
            //获取skuName的高亮
            Map<String, List<String>> highlight = hit.highlight;
            if (highlight != null && highlight.size() > 0) {
                //有高亮，则从高亮中取数据
                List<String> skuName = highlight.get("skuName");
                String highlightSkuName = skuName.get(0);//表示获取集合中的第一个数据，因为这个集合只有一个数据
                source.setSkuName(highlightSkuName);
            }
            skuLsInfoList.add(source);
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoList);

        //2. 给SkuLsResult的 total赋值
        skuLsResult.setTotal(skuLsResult.getTotal());

        //3. 给SkuLsResult的 totalPages 赋值
        //取记录个数并计算出总页数
        long totalPage = (searchResult.getTotal() + skuLsParams.getPageSize() - 1) / skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPage);

        //4. 给SkuLsResult的 attrValueIdList 赋值
        List<String> attrValueIdList = new ArrayList<>();
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        if (groupby_attr != null) {
            List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
            for (TermsAggregation.Entry bucket : buckets) {
                attrValueIdList.add(bucket.getKey());
            }
            skuLsResult.setAttrValueIdList(attrValueIdList);
        }
            return skuLsResult;
        }

        /**
         * 完全根据手写的dsl语句
         * @param skuLsParams
         * @return
         */
        private String makeQueryStringForSearch (SkuLsParams skuLsParams){
            //定义一个查询器
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

            //创建bool
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

            String keyword = skuLsParams.getKeyword();
            //判断keyword是否为空
            if (keyword != null && keyword.length() > 0) {
                MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", keyword);
                //创建must
                boolQueryBuilder.must(matchQueryBuilder);
                //设置高亮
                HighlightBuilder highlighter = searchSourceBuilder.highlighter();
                //设置高亮规则
                highlighter.preTags("<span style=color:red>");
                highlighter.postTags("</span>");
                highlighter.field("skuName");
                //高亮规则放入到查询器中
                SearchSourceBuilder highlight = searchSourceBuilder.highlight(highlighter);
            }

            String[] valueIds = skuLsParams.getValueId();
            //判断平台属性值id
            if (valueIds != null && valueIds.length > 0) {
                //循环
                for (String valueId : valueIds) {
                    //创建term
                    TermQueryBuilder termQueryBuilder =
                            new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                    //创建一个filter,并添加term
                    boolQueryBuilder.filter(termQueryBuilder);
                }
            }

            //判断三级属性id
            if (skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0) {
                //创建term
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
                //创建一个filter,并添加term
                boolQueryBuilder.filter(termQueryBuilder);
            }
            //query
            searchSourceBuilder.query(boolQueryBuilder);

            //设置分页
            int fromPageno = (skuLsParams.getPageNo() - 1) * skuLsParams.getPageSize();
            searchSourceBuilder.from(fromPageno);//从第几条开始查询
            searchSourceBuilder.size(skuLsParams.getPageSize());//每一页显示的条数

            //设置排序
            searchSourceBuilder.sort("hotScore", SortOrder.DESC);//降序

            //聚合
            TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr");

            //"field":"skuAttrValueList.valueId"
            groupby_attr.field("skuAttrValueList.valueId");
            searchSourceBuilder.aggregation(groupby_attr);

            String query = searchSourceBuilder.toString();
            return query;
        }
    }
