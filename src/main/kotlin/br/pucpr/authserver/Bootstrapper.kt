package br.pucpr.authserver

import br.pucpr.authserver.boardgames.Tag
import br.pucpr.authserver.boardgames.TagRepository
import br.pucpr.authserver.nights.Member
import br.pucpr.authserver.nights.MemberRepository
import br.pucpr.authserver.roles.Role
import br.pucpr.authserver.roles.RoleRepository
import br.pucpr.authserver.users.User
import br.pucpr.authserver.users.UserRepository
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component

@Component
class Bootstrapper(
    val rolesRepository: RoleRepository,
    val userRepository: UserRepository,
    val tagRepository: TagRepository,
    val memberRepository: MemberRepository,
) : ApplicationListener<ContextRefreshedEvent> {
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        //Cria os papéis ADMIN e PREMIUM USER, se não existirem
        val adminRole =
            rolesRepository.findByName("ADMIN") ?: rolesRepository
                .save(Role(name = "ADMIN", description = "System Administrator"))
        rolesRepository.findByName("PREMIUM") ?: rolesRepository
            .save(Role(name = "PREMIUM", description = "Premium user"))

        //Cria um admin se não existir nenhum
        if (userRepository.findByRole("ADMIN").isEmpty()) {
            val admin = User(
                email = "admin@authserver.com",
                password = "admin",
                name = "Auth Server Administrator",
            )
            admin.roles.add(adminRole)
            userRepository.save(admin)
        }

        listOf(
            "party" to "Party",
            "euro" to "Euro",
            "coop" to "Coop",
            "deducao" to "Dedução",
        ).forEach { (slug, label) ->
            if (tagRepository.findBySlug(slug) == null) {
                tagRepository.save(Tag(slug = slug, label = label))
            }
        }
        listOf(
            "Lucas" to "lucas@pucpr.edu.br",
            "Ana" to "ana@pucpr.edu.br",
            "Bruno" to "bruno@pucpr.edu.br",
            "Carla" to "carla@pucpr.edu.br",
        ).forEach { (displayName, email) ->
            if (memberRepository.findByDisplayName(displayName) == null) {
                memberRepository.save(
                    Member(
                        displayName = displayName,
                        email = email,
                        seasonPoints = 0,
                    )
                )
            }
        }
    }
}