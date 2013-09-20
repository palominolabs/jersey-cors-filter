package com.palominolabs.jersey.cors

import com.google.common.collect.Maps
import com.sun.jersey.api.core.DefaultResourceConfig
import org.junit.Test

import static com.palominolabs.jersey.cors.Ternary.FALSE
import static com.palominolabs.jersey.cors.Ternary.TRUE

class CorsResourceFilterFactoryTest {
  @Test
  public void testStandardDefaults() {
    CorsResourceFilterFactory factory = new CorsResourceFilterFactory(new DefaultResourceConfig())

    assert '*' == factory.defAllowOrigin
    assert '' == factory.defExposeHeaders
    assert 24 * 3600 == factory.defMaxAge
    assert FALSE == factory.defAllowCredentials
    assert '' == factory.defAllowMethods
    assert '' == factory.defAllowHeaders
  }

  @Test
  public void testPropertiesWithExactTypes() {
    Map<String, Object> props = Maps.newHashMap()

    props.put(CorsConfig.ALLOW_ORIGIN, 'http://foo.com')
    props.put(CorsConfig.EXPOSE_HEADERS, 'x-foo')
    props.put(CorsConfig.MAX_AGE, 12345)
    props.put(CorsConfig.ALLOW_CREDENTIALS, true)
    props.put(CorsConfig.ALLOW_METHODS, 'POST')
    props.put(CorsConfig.ALLOW_HEADERS, 'x-bar')

    assertOverriddenDefaults(props)
  }

  @Test
  public void testPropertiesWithStrings() {
    Map<String, Object> props = Maps.newHashMap()

    props.put(CorsConfig.ALLOW_ORIGIN, 'http://foo.com')
    props.put(CorsConfig.EXPOSE_HEADERS, 'x-foo')
    props.put(CorsConfig.MAX_AGE, '12345')
    props.put(CorsConfig.ALLOW_CREDENTIALS, 'true')
    props.put(CorsConfig.ALLOW_METHODS, 'POST')
    props.put(CorsConfig.ALLOW_HEADERS, 'x-bar')

    assertOverriddenDefaults(props)
  }

  private static void assertOverriddenDefaults(HashMap<String, Object> props) {
    DefaultResourceConfig config = new DefaultResourceConfig()
    config.setPropertiesAndFeatures(props)

    CorsResourceFilterFactory factory = new CorsResourceFilterFactory(config)

    assert 'http://foo.com' == factory.defAllowOrigin
    assert 'x-foo' == factory.defExposeHeaders
    assert 12345 == factory.defMaxAge
    assert TRUE == factory.defAllowCredentials
    assert 'POST' == factory.defAllowMethods
    assert 'x-bar' == factory.defAllowHeaders
  }
}
