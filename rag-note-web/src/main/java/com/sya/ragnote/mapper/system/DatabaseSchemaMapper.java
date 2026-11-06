package com.sya.ragnote.mapper.system;

import org.apache.ibatis.annotations.Select;

public interface DatabaseSchemaMapper {
    @Select(
            "SELECT a.atttypmod FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN"
                + " pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=current_schema() AND"
                + " c.relname='note_chunk' AND a.attname='embedding' AND NOT a.attisdropped")
    Integer embeddingDimensions();
}
