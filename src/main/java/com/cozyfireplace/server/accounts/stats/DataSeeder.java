package com.cozyfireplace.server.accounts.stats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final StatRepository statRepository;
    private final SkillRepository skillRepository;

    @Bean
    @Transactional
    public ApplicationRunner seedDndData() {
        return args -> {
            if (statRepository.count() == 0) {
                seedStats();
            }
            if (skillRepository.count() == 0) {
                seedSkills();
            }
        };
    }

    private void seedStats() {
        Stat str = Stat.builder().key("str").label("Сила").system(GameSystem.DND_5E).sortOrder(0).build();
        Stat dex = Stat.builder().key("dex").label("Ловкость").system(GameSystem.DND_5E).sortOrder(1).build();
        Stat con = Stat.builder().key("con").label("Телосложение").system(GameSystem.DND_5E).sortOrder(2).build();
        Stat intel = Stat.builder().key("int").label("Интеллект").system(GameSystem.DND_5E).sortOrder(3).build();
        Stat wis = Stat.builder().key("wis").label("Мудрость").system(GameSystem.DND_5E).sortOrder(4).build();
        Stat cha = Stat.builder().key("cha").label("Харизма").system(GameSystem.DND_5E).sortOrder(5).build();

        statRepository.saveAll(java.util.List.of(str, dex, con, intel, wis, cha));
        log.info("Seeded 6 D&D stats");
    }

    private void seedSkills() {
        Stat str = statRepository.findById("str").orElseThrow();
        Stat dex = statRepository.findById("dex").orElseThrow();
        Stat con = statRepository.findById("con").orElseThrow();
        Stat intel = statRepository.findById("int").orElseThrow();
        Stat wis = statRepository.findById("wis").orElseThrow();
        Stat cha = statRepository.findById("cha").orElseThrow();

        java.util.List<Skill> skills = java.util.List.of(
                Skill.builder().key("acrobatics").label("Акробатика").system(GameSystem.DND_5E).stat(dex).sortOrder(0).build(),
                Skill.builder().key("animalHandling").label("Уход за животными").system(GameSystem.DND_5E).stat(wis).sortOrder(1).build(),
                Skill.builder().key("arcana").label("Магия").system(GameSystem.DND_5E).stat(intel).sortOrder(2).build(),
                Skill.builder().key("athletics").label("Атлетика").system(GameSystem.DND_5E).stat(str).sortOrder(3).build(),
                Skill.builder().key("deception").label("Обман").system(GameSystem.DND_5E).stat(cha).sortOrder(4).build(),
                Skill.builder().key("history").label("История").system(GameSystem.DND_5E).stat(intel).sortOrder(5).build(),
                Skill.builder().key("insight").label("Проницательность").system(GameSystem.DND_5E).stat(wis).sortOrder(6).build(),
                Skill.builder().key("intimidation").label("Запугивание").system(GameSystem.DND_5E).stat(cha).sortOrder(7).build(),
                Skill.builder().key("investigation").label("Анализ").system(GameSystem.DND_5E).stat(intel).sortOrder(8).build(),
                Skill.builder().key("medicine").label("Медицина").system(GameSystem.DND_5E).stat(wis).sortOrder(9).build(),
                Skill.builder().key("nature").label("Природа").system(GameSystem.DND_5E).stat(intel).sortOrder(10).build(),
                Skill.builder().key("perception").label("Внимательность").system(GameSystem.DND_5E).stat(wis).sortOrder(11).build(),
                Skill.builder().key("performance").label("Выступление").system(GameSystem.DND_5E).stat(cha).sortOrder(12).build(),
                Skill.builder().key("persuasion").label("Убеждение").system(GameSystem.DND_5E).stat(cha).sortOrder(13).build(),
                Skill.builder().key("religion").label("Религия").system(GameSystem.DND_5E).stat(intel).sortOrder(14).build(),
                Skill.builder().key("sleightOfHand").label("Ловкость рук").system(GameSystem.DND_5E).stat(dex).sortOrder(15).build(),
                Skill.builder().key("stealth").label("Скрытность").system(GameSystem.DND_5E).stat(dex).sortOrder(16).build(),
                Skill.builder().key("survival").label("Выживание").system(GameSystem.DND_5E).stat(wis).sortOrder(17).build()
        );

        skillRepository.saveAll(skills);
        log.info("Seeded 18 D&D skills");
    }
}
