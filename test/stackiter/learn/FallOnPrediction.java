package stackiter.learn;

import org.junit.*;

public class FallOnPrediction {

	@Test
	public void fallOn() {
		Sequence sequence = new Loader().loadResource(
			"stackiter-20101104-215911-501_drop-from-25.log.gz"
		);
	}

}
