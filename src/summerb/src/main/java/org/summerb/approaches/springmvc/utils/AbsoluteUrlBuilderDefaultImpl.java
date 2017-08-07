package org.summerb.approaches.springmvc.utils;

import javax.servlet.http.HttpServletRequest;

public class AbsoluteUrlBuilderDefaultImpl implements AbsoluteUrlBuilder {
	@Override
	public String buildExternalUrl(String optionalRelativeUrl) {
		HttpServletRequest req = CurrentRequestUtils.get();
		String ret = req.getScheme() + "://" + req.getServerName();
		if (!(((req.getServerPort() == 80 && req.getScheme().equalsIgnoreCase("http"))
				|| (req.getServerPort() == 443 && req.getScheme().equalsIgnoreCase("https"))))) {
			ret += ":" + req.getServerPort();
		}
		ret += req.getContextPath();
		return ret;
	}
}
