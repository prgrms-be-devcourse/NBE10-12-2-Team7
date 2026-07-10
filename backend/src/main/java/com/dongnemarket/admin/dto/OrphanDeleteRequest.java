package com.dongnemarket.admin.dto;

import java.util.List;

public record OrphanDeleteRequest(List<OrphanTarget> targets) {

    public record OrphanTarget(String directory, String filename) {
    }
}
