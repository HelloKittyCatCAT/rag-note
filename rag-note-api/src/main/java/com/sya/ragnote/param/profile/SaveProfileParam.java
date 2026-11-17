package com.sya.ragnote.param.profile;

import jakarta.validation.constraints.Size;

public record SaveProfileParam(
        boolean enabled,
        @Size(max = 500) String learningGoal,
        @Size(max = 500) String explanationPreference) {}
