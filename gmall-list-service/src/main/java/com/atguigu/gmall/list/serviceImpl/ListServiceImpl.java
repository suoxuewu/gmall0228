package com.atguigu.gmall.list.serviceImpl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {
    @Autowired
    JestClient jestClient;
    // 定义一些 index,type.
    public static final String ES_INDEX="gmall";
    public static final String ES_TYPE="SkuInfo";
    private HighlightBuilder highlightBuilder;

    @Override
    public void saveSkulsInfo(SkuLsInfo skuLsInfo) {
        Index index = new Index.Builder(skuLsInfo)
                .index(ES_INDEX).type(ES_INDEX)
                .id(skuLsInfo.getId()).build();
        try {
            DocumentResult documentResult = jestClient.execute(index);
            System.out.println(documentResult);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        // 制作query 字符串
        String query = makeQueryStringForSearch(skuLsParams);
        // 准备执行搜索
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult = null;
        try {
            searchResult = jestClient.execute(search);
            System.out.println("searchResult="+searchResult);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 将搜索结果转换成我们自定义java类型SkuLsResult.

        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams,searchResult);
        System.out.println("skuLsResult="+skuLsResult);
        return skuLsResult;
    }
    // 数据集转换方法！
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        SkuLsResult skuLsResult = new SkuLsResult();
        // 从查询到的结果集中取数据
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        // 创建一个新的集合来存放新的实体对象
        ArrayList<SkuLsInfo> skuLsInfoArrayList = new ArrayList<>();
        // 循环遍历 iter
        if (hits!=null && hits.size()>0){
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                // 取得对象
                SkuLsInfo skuLsInfo = hit.source;
                // 准备取得高亮
                if (hit.highlight!=null && hit.highlight.size()>0){
                    // 取出一个集合
                    List<String> list = hit.highlight.get("skuName");
                    // 取得高亮后的名称，给对象
                    String skuNameHi = list.get(0);
                    skuLsInfo.setSkuName(skuNameHi);
                }
                // 循环一条添加一条
                skuLsInfoArrayList.add(skuLsInfo);
            }
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoArrayList);
        // 总条数
        skuLsResult.setTotal(searchResult.getTotal());
        // 总页数
        // long pages = searchResult.getTotal()%skuLsParams.getPageSize()==0?searchResult.getTotal()/skuLsParams.getPageSize():searchResult.getTotal()/skuLsParams.getPageSize()+1;
        // (total+size-1)/size;
        long pages = (searchResult.getTotal()+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
        skuLsResult.setTatalPages(pages);

        // 聚合
        MetricAggregation aggregations = searchResult.getAggregations();
        // 按照名称取得数据groupby_attr
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");

        // 取得buckets
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();

        // 创建一个集合
        ArrayList<String> valusList = new ArrayList<>();
        if (buckets!=null && buckets.size()>0){
            for (TermsAggregation.Entry bucket : buckets) {
                // 取得数据
                String valueId = bucket.getKey();
                valusList.add(valueId);
            }
        }
        skuLsResult.setAttrValueIdList(valusList);
        return skuLsResult;
    }
    //返回query查询字符串
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        // 先构建一个查询器 solr
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 创建bool对象
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 判断关键字 ,后续会根据skuName ,进行查询，以及对他进行高亮。
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            // 创建match，并添加到bool对象中
            MatchQueryBuilder matchQueryBuilder =
                    new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            boolQueryBuilder.must(matchQueryBuilder);
            // 准备设置高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuName");
            highlightBuilder.preTags("<span style='color:red'>");
            highlightBuilder.postTags("</span>");
            searchSourceBuilder.highlight(highlightBuilder);
        }

        // 整valuesId,catalog3Id
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            TermQueryBuilder termQueryBuilder =
                    new TermQueryBuilder
                            ("catalog3Id",skuLsParams.getCatalog3Id());

            boolQueryBuilder.filter(termQueryBuilder);
        }

        if (skuLsParams.getValueId()!=null
                && skuLsParams.getValueId().length>0){
            // 遍历添加 fori
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                // 取得每一个对象的Id
                String valusId = skuLsParams.getValueId()[i];
                TermQueryBuilder termQueryBuilder =
                        new TermQueryBuilder("skuAttrValueList.valueId",valusId);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        // 分页 公式： pageSize *(pageNo-1)
        int from = (skuLsParams.getPageNo()-1)*skuLsParams.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(skuLsParams.getPageSize());
        // 排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        // 聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);
        // 将整个的dsl的拼接语句执行
        searchSourceBuilder.query(boolQueryBuilder);
        String query = searchSourceBuilder.toString();
        System.out.println("query:="+query);
        return query;
    }
}
