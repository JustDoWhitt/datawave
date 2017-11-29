package nsa.datawave.query.rewrite.iterator.pipeline;

import nsa.datawave.query.rewrite.iterator.YieldCallbackWrapper;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;

import nsa.datawave.query.rewrite.iterator.NestedIterator;
import nsa.datawave.query.rewrite.iterator.QueryIterator;
import nsa.datawave.query.rewrite.iterator.profile.QuerySpan;
import nsa.datawave.query.rewrite.iterator.profile.QuerySpanCollector;

public class PipelineFactory {
    
    /**
     * Create a pipeline iterator.
     * 
     * @param documents
     *            Document Iterator.
     * @param maxPipelines
     *            maximum number of requested pipelines.
     * @param maxCachedResults
     *            maximum cached results.
     * @param requestSerialPipeline
     *            request for a serial pipeline. In the future this choice may not be honored
     * @param querySpanCollector
     *            query span collector
     * @param querySpan
     *            query span
     * @param sourceIterator
     *            source iterator.
     * @param sourceForDeepCopy
     *            source used for deep copies.
     * @param env
     *            iterator environment
     * @return
     */
    public static PipelineIterator createIterator(NestedIterator<Key> documents, int maxPipelines, int maxCachedResults, boolean requestSerialPipeline,
                    QuerySpanCollector querySpanCollector, QuerySpan querySpan, QueryIterator sourceIterator,
                    SortedKeyValueIterator<Key,Value> sourceForDeepCopy, IteratorEnvironment env, YieldCallbackWrapper<Key> yield, long yieldThresholdMs) {
        if (maxPipelines > 1 && !requestSerialPipeline) {
            return new PipelineIterator(documents, maxPipelines, maxCachedResults, querySpanCollector, querySpan, sourceIterator, sourceForDeepCopy, env,
                            yield, yieldThresholdMs);
        } else {
            return new SerialIterator(documents, maxPipelines, maxCachedResults, querySpanCollector, querySpan, sourceIterator, sourceForDeepCopy, env, yield,
                            yieldThresholdMs);
        }
        
    }
    
}