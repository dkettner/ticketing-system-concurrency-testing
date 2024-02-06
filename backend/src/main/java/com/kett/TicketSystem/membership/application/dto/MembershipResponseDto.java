package com.kett.TicketSystem.membership.application.dto;

import com.kett.TicketSystem.membership.domain.Role;
import com.kett.TicketSystem.membership.domain.State;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MembershipResponseDto {
    private UUID id;
    private UUID projectId;
    private UUID userId;
    private Role role;
    private State state;
}
