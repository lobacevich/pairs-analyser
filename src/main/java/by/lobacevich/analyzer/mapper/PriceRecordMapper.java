package by.lobacevich.analyzer.mapper;

import by.lobacevich.analyzer.dto.PriceDtoResponse;
import by.lobacevich.analyzer.entity.PriceRecord;
import org.mapstruct.Mapper;

@Mapper
public interface PriceRecordMapper {

    PriceDtoResponse entityToDto(PriceRecord record);
}
