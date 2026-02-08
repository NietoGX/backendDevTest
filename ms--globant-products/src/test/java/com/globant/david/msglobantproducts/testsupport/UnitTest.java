package com.globant.david.msglobantproducts.testsupport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for unit tests.
 * Provides common configuration for all unit tests including:
 * - Mockito extension for mocking support
 * - Standardized display name format
 * <p>
 * Unit tests should extend this class to inherit these configurations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests")
public abstract class UnitTest {
}
