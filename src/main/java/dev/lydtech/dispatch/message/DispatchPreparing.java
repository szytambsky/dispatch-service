package dev.lydtech.dispatch.message;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DispatchPreparing {
    UUID orderId;
}
