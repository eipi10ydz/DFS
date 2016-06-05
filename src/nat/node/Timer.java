package data_transferor;
/**
 * @author wzy
 * @version 1.0
 */

class Timer {

	private long start_time;
	private int period, max_period;
	private int cnt;

	/**
	 * @param period
	 *            period in milliseconds
	 * @throws IllegalArgumentException
	 *             if {@code period <= 0}.
	 */
	public Timer(int period) {
		if (period <= 0)
			throw new IllegalArgumentException("Non-positive period.");
		this.period = period;
		this.max_period = -1;
		this.start_time = 0;
		this.cnt = 0;
	}

	/**
	 * @param period
	 *            period in milliseconds
	 * @param longest_period
	 *            longest period in milliseconds
	 * @throws IllegalArgumentException
	 *             if {@code period <= 0} or {@code longest_period <= 0}.
	 */
	public Timer(int period, int longest_period) {
		if (period <= 0)
			throw new IllegalArgumentException("Non-positive period.");
		if (longest_period <= 0)
			throw new IllegalArgumentException("Non-positive longest period.");
		this.period = period;
		this.max_period = longest_period;
		this.start_time = 0;
		this.cnt = 0;
	}

	/**
	 * 
	 */
	public void start() {
		this.start_time = System.currentTimeMillis();
		this.cnt++;
		return;
	}

	/**
	 * 
	 */
	public void reset() {
		this.start_time = 0;
		this.cnt = 0;

	}

	/**
	 * @param period
	 *            period in milliseconds
	 * @throws IllegalArgumentException
	 *             if {@code period <= 0}.
	 */
	public void setPeriod(int period) {
		if (period <= 0)
			throw new IllegalArgumentException("Non-positive period.");
		this.period = period;
		return;
	}

	/**
	 * @param longest_period
	 *            longest period in milliseconds
	 * @throws IllegalArgumentException
	 *             if {@code longest_period <= 0}.
	 */
	public void setLongestPeriod(int longest_period) {
		if (longest_period <= 0)
			throw new IllegalArgumentException("Non-positive longest period.");
		this.max_period = longest_period;
		return;
	}

	/**
	 * @return
	 */
	public int getCnt() {
		return cnt;
	}

	/**
	 * @param delay
	 *            delay in milliseconds
	 */
	public void postpone(int delay) {
		this.period += delay;
		if (this.max_period > 0 && this.period > this.max_period)
			this.period = this.max_period;
		return;
	}

	/**
	 * @return if the timer has expired
	 */
	public boolean isExpired() {
		return (this.start_time + this.period) >= System.currentTimeMillis();
	}

}
