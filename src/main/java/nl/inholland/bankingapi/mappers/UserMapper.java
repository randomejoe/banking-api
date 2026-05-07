package nl.inholland.bankingapi.mappers;

import nl.inholland.bankingapi.dtos.UserResponse;
import nl.inholland.bankingapi.entities.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
