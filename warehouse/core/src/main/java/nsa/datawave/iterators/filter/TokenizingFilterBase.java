package nsa.datawave.iterators.filter;

import java.util.Objects;
import nsa.datawave.iterators.filter.ageoff.AgeOffPeriod;
import nsa.datawave.iterators.filter.ageoff.AppliedRule;
import nsa.datawave.iterators.filter.ageoff.FilterOptions;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;

/**
 * TokenizingAgeoffFilter cuts a field into tokens (splitting at a specified set of delimiters), and makes ageoff decisions based on whether or not any of the
 * supplied tokens are in its configuration. If multiple of the configured tokens are found among the field's tokens, the TTL of the token appearing first in
 * the configuration is applied.
 * <p/>
 * This can be used to specify ageoff based on a field with, for example, comma separated values.
 * <p/>
 * A sample configuration might look like:
 *
 * <pre>
 * &lt;rule&gt;
 *   &lt;filterClass&gt;nsa.datawave.iterators.filter.ColumnQualifierTokenFilter&lt;/filterClass&gt;
 *   &lt;!-- Any tokens without specified TTLs will ageoff after 3000ms --&lt;
 *   &lt;ttl units="ms"&gt;3000&lt;/ttl&gt;
 * 
 *   &lt;!-- Field is comma separated --&gt;
 *   &lt;delimiters&gt;,&lt;/delimiters&gt;
 * 
 *   &lt;matchPattern&gt;
 *     "foo": 5d,
 *     "bar": 300ms,
 *     "baz", &lt;!-- use rule's default TTL --&gt;
 *   &lt;/matchPattern&gt;
 * &lt;/rule&gt;
 * </pre>
 * <p/>
 * With such a configuration:
 * <ul>
 * <li>a cell with column qualifier foo,bar would be aged off after 5 days ('foo' wins, since it appears first in configuration),</li>
 * <li>a cell with column qualifier foobar,baz would be aged off after 3000ms (only the token 'baz' appears in configuration),</li>
 * <li>a cell with column qualifier baz,bar would be aged off after 300 ms ('bar' wins, since it appears first in configuration), and</li>
 * <li>a cell with column qualifier foobar,barbaz would not be assigned a TTL by this filter.
 * </ul>
 *
 */
public abstract class TokenizingFilterBase extends AppliedRule {
    public final static String DELIMITERS_TAG = "delimiters";
    
    private String matchPattern = null;
    private TokenTtlTrie scanTrie = null;
    private boolean ruleApplied;
    
    public TokenizingFilterBase() {
        super();
    }
    
    public abstract byte[] getKeyField(Key k, Value V);
    
    /**
     * Return a list of delimiters for scans. While the default is to pull this information out of the {@code &lt;delimiters&gt;} tag in the configuration,
     * subclasses may wish to override this to provide fixed delimiter sets.
     */
    public byte[] getDelimiters(FilterOptions options) {
        String delimiters = options.getOption(DELIMITERS_TAG);
        if (delimiters == null) {
            throw new IllegalArgumentException("A set of delimiters must be specified");
        }
        return delimiters.getBytes();
    }
    
    @Override
    public void init(FilterOptions options) {
        super.init(options);
        ruleApplied = false;
        String confPattern = options.getOption(AgeOffConfigParams.MATCHPATTERN);
        if (!Objects.equals(matchPattern, confPattern)) {
            this.scanTrie = new TokenTtlTrie.Builder().setDelimiters(getDelimiters(options)).parse(confPattern).build();
            this.matchPattern = confPattern;
        }
    }
    
    @Override
    public void deepCopyInit(FilterOptions newOptions, AppliedRule parentCopy) {
        TokenizingFilterBase parent = (TokenizingFilterBase) parentCopy;
        this.matchPattern = parent.matchPattern;
        this.scanTrie = parent.scanTrie;
        super.deepCopyInit(newOptions, parentCopy);
    }
    
    @Override
    public boolean accept(AgeOffPeriod period, Key k, Value V) {
        Long calculatedTTL = scanTrie.scan(getKeyField(k, V));
        if (calculatedTTL == null) {
            ruleApplied = false;
            return true;
        }
        long cutoffTimestamp = period.getCutOffMilliseconds();
        if (calculatedTTL > 0) {
            cutoffTimestamp -= calculatedTTL - period.getTtl() * period.getTtlUnitsFactor();
        }
        ruleApplied = true;
        return k.getTimestamp() > cutoffTimestamp;
    }
    
    @Override
    public boolean isFilterRuleApplied() {
        return ruleApplied;
    }
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[size=" + (scanTrie == null ? null : scanTrie.size()) + "]";
    }
}